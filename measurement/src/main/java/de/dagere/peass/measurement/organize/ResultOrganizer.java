package de.dagere.peass.measurement.organize;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.analysis.MultipleVMTestUtil;

public class ResultOrganizer {

   private static final Logger LOG = LogManager.getLogger(ResultOrganizer.class);

   protected final PeassFolders folders;
   // mainVersion equals current version
   private final String mainVersion;
   private final long currentChunkStart;
   private final boolean isUseKieker;

   private final boolean saveAll;
   protected final TestCase testcase;
   private boolean success = true;
   private final int expectedIterations;
   private final KiekerFileCompressor compressor = new KiekerFileCompressor();

   public ResultOrganizer(final PeassFolders folders, final String currentVersion, final long currentChunkStart, final boolean isUseKieker, final boolean saveAll,
         final TestCase test,
         final int expectedIterations) {
      this.folders = folders;
      this.mainVersion = currentVersion;
      this.currentChunkStart = currentChunkStart;
      this.isUseKieker = isUseKieker;
      this.saveAll = saveAll;
      this.testcase = test;
      this.expectedIterations = expectedIterations;
   }

   // TODO the success test duplicatees saveResultFiles code and logic from DependencyTester.shouldReduce
   /**
    * Tests whether there is a correct result file, i.e. a XML file in the correct position with the right amount of iterations (it may be less iterations if the test takes too
    * long). This only works *before* the result has been moved, afterwards, the file will be gone and the measurement will be considered no success
    * 
    * @return true of the measurement was correct
    */
   public boolean testSuccess(final String version) {
      final File folder = getTempResultsFolder(version);
      if (folder != null) {
         final String methodname = testcase.getMethod();
         final File oneResultFile = new File(folder, methodname + ".xml");
         try {
            if (!oneResultFile.exists()) {
               success = false;
               LOG.error("Result file {} does not exist - probably timeout", oneResultFile);
            } else {
               LOG.debug("Reading: {}", oneResultFile);
               XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
               final Kopemedata oneResultData = xdl.getFullData();
               final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
               if (testcaseList.size() > 0) {
                  Result result = oneResultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
                  if (result.getIterations() == expectedIterations) {
                     success = true;
                  } else {
                     success = false;
                     LOG.error("Wrong execution count: {} Expected: {}", result.getIterations(), expectedIterations);
                  }
               } else {
                  LOG.error("Testcase not found in XML");
                  success = false;
               }
            }
         } catch (JAXBException e) {
            e.printStackTrace();
            success = false;
         }
      } else {
         LOG.error("Folder {} does not exist", folder);
         success = false;
      }
      return success;
   }

   public void saveResultFiles(final String version, final int vmid)
         throws JAXBException, IOException {
      // Saving and merging result files should not be executed in parallel, therefore, this needs to be synchronized over the class (not the instance)
      synchronized (ResultOrganizer.class) {
         final File folder = getTempResultsFolder(version);
         if (folder != null) {
            final String methodname = testcase.getMethod();
            final File oneResultFile = new File(folder, methodname + ".xml");
            if (!oneResultFile.exists()) {
               LOG.debug("File {} does not exist.", oneResultFile.getAbsolutePath());
               success = false;
            } else {
               LOG.debug("Reading: {}", oneResultFile);
               final XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
               final Kopemedata oneResultData = xdl.getFullData();
               final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
               if (testcaseList.size() > 0) {
                  saveResults(version, vmid, oneResultFile, oneResultData, testcaseList);

                  if (isUseKieker) {
                     File destFolder = folders.getFullResultFolder(testcase, mainVersion, version);
                     saveKiekerFiles(folder, destFolder);
                  }
               } else {
                  LOG.error("No data - measurement failed?");
                  success = false;
               }
            }
            for (final File file : folder.listFiles()) {
               FileUtils.forceDelete(file);
            }
         }
      }
   }

   public File getTempResultsFolder(final String version) {
      LOG.info("Searching method: {}", testcase);
      final Collection<File> folderCandidates = folders.findTempClazzFolder(testcase);
      if (folderCandidates.size() != 1) {
         LOG.error("Ordner {} ist {} mal vorhanden.", testcase.getClazz(), folderCandidates.size());
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }

   private void saveResults(final String version, final int vmid, final File oneResultFile,
         final Kopemedata oneResultData, final List<TestcaseType> testcaseList)
         throws JAXBException, IOException {
      // Update testname, in case it has been set to
      // testRepetition
      testcaseList.get(0).setName(testcase.getMethod());

      saveSummaryFile(version, testcaseList, oneResultFile);

      final File destFile = folders.getResultFile(testcase, vmid, version, mainVersion);
      destFile.getParentFile().mkdirs();
      LOG.info("Saving in: {}", destFile);
      if (!destFile.exists()) {
         final Fulldata fulldata = oneResultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0).getFulldata();
         if (fulldata.getFileName() != null) {
            saveFulldataFile(vmid, oneResultFile, destFile, fulldata);
         }
         XMLDataStorer.storeData(destFile, oneResultData);
         oneResultFile.delete();
      } else {
         throw new RuntimeException("Moving failed: " + destFile + " already exist.");
      }
   }

   private void saveFulldataFile(final int vmid, final File oneResultFile, final File destFile, final Fulldata fulldata) throws IOException {
      File fulldataFile = new File(oneResultFile.getParentFile(), fulldata.getFileName());
      final String destFileName = testcase.getMethod() + "_kopeme_" + vmid + ".tmp";
      File destKopemeFile = new File(destFile.getParentFile(), destFileName);
      Files.move(fulldataFile.toPath(), destKopemeFile.toPath());
      fulldata.setFileName(destFileName);
   }

   public void saveSummaryFile(final String version, final List<TestcaseType> testcaseList, final File oneResultFile) throws JAXBException {
      final TestcaseType oneRundata = testcaseList.get(0);
      final File fullResultFile = folders.getFullSummaryFile(testcase);
      MultipleVMTestUtil.saveSummaryData(fullResultFile, oneResultFile, oneRundata, testcase, version, currentChunkStart);
   }

   private void saveKiekerFiles(final File folder, final File destFolder) throws IOException {
      final File[] kiekerFolders = folder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"));
      if (kiekerFolders.length != 1) {
         String fileNameList = Arrays.toString(kiekerFolders);
         throw new RuntimeException("It is expected that after one execution exactly one Kieker folder exists, but was " + fileNameList);
      }
      if (saveAll) {
         compressor.moveOrCompressFile(destFolder, kiekerFolders[0]);
      } else {
         FileUtils.deleteDirectory(kiekerFolders[0]);
      }
   }

   public KiekerFileCompressor getCompressor() {
      return compressor;
   }
   
   public File getResultFile(final TestCase testcase, final int vmid, final String version) {
      return folders.getResultFile(testcase, vmid, version, mainVersion);
   }

   public boolean isSaveAll() {
      return saveAll;
   }

   public TestCase getTest() {
      return testcase;
   }

   public boolean isSuccess() {
      return success;
   }
}
