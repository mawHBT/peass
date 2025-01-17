package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestExistenceChanges;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;

public class TraceChangeHandler {

   private static final boolean DETAIL_DEBUG = true;

   private static final Logger LOG = LogManager.getLogger(TraceChangeHandler.class);

   private final DependencyManager dependencyManager;
   private final PeassFolders folders;
   private final ExecutionConfig executionConfig;
   private final String version;

   public TraceChangeHandler(final DependencyManager dependencyManager, final PeassFolders folders, final ExecutionConfig executionConfig,
         final String version) {
      this.dependencyManager = dependencyManager;
      this.folders = folders;
      this.executionConfig = executionConfig;
      this.version = version;
   }

   public void handleTraceAnalysisChanges(final Version newVersionInfo)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException {
      LOG.debug("Updating dependencies.. {}", version);

      final TestSet testsToRun = getTestsToRun(newVersionInfo);

      if (testsToRun.classCount() > 0) {
         analyzeTests(newVersionInfo, testsToRun);
      }
   }

   private TestSet getTestsToRun(final Version newVersionInfo) throws IOException, JsonGenerationException, JsonMappingException {
      final TestSet testsToRun = newVersionInfo.getTests() ; // contains only the tests that need to be run -> could be changeTestMap.values() und dann
                                                                                      // umwandeln
      addAddedTests(newVersionInfo, testsToRun);
      
      Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "toRun_" + version + ".json"), testsToRun.entrySet());

      NonIncludedTestRemover.removeNotIncluded(testsToRun, executionConfig);
      return testsToRun;
   }

   private void addAddedTests(final Version newVersionInfo, final TestSet testsToRun) {
      for (final ChangedEntity testName : newVersionInfo.getChangedClazzes().keySet()) {
         if (testName.getJavaClazzName().toLowerCase().contains("test")) {
            ChangedEntity simplyClazz = testName.getSourceContainingClazz();
            testsToRun.addTest(simplyClazz, null);
         }
      }
   }

   private void analyzeTests(final Version newVersionInfo, final TestSet testsToRun)
         throws IOException, XmlPullParserException, InterruptedException, JsonGenerationException, JsonMappingException {
      final ModuleClassMapping mapping = new ModuleClassMapping(dependencyManager.getExecutor());
      dependencyManager.runTraceTests(testsToRun, version);

      handleDependencyChanges(newVersionInfo, testsToRun, mapping);
   }

   private void handleDependencyChanges(final Version newVersionInfo, final TestSet testsToRun, final ModuleClassMapping mapping)
         throws IOException, XmlPullParserException, JsonGenerationException, JsonMappingException {
      final TestExistenceChanges testExistenceChanges = dependencyManager.updateDependencies(testsToRun, version, mapping);
      final Map<ChangedEntity, Set<ChangedEntity>> newTestcases = testExistenceChanges.getAddedTests();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "add_" + version + ".json"), newTestcases);
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "remove_" + version + ".json"), testExistenceChanges.getRemovedTests());
      }

      DependencyReaderUtil.removeDeletedTestcases(newVersionInfo, testExistenceChanges);
      DependencyReaderUtil.addNewTestcases(newVersionInfo, newTestcases);

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "final_" + version + ".json"), newVersionInfo);
   }
}
