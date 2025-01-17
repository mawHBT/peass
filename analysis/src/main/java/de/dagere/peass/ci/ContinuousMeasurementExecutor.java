package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependencyprocessors.AdaptiveTester;
import de.dagere.peass.folders.PeassFolders;

public class ContinuousMeasurementExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousMeasurementExecutor.class);

   private final String version, versionOld;
   private final PeassFolders folders;
   private final MeasurementConfig measurementConfig;
   private final EnvironmentVariables env;

   public ContinuousMeasurementExecutor(final String version, final String versionOld, final PeassFolders folders, final MeasurementConfig measurementConfig,
         final EnvironmentVariables env) {
      this.version = version;
      this.versionOld = versionOld;
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
   }

   public File executeMeasurements(final Set<TestCase> tests, final File fullResultsVersion, final File logFile) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      if (!fullResultsVersion.exists()) {
         if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
            LOG.info("Executing measurement - Log goes to {}", logFile.getAbsolutePath());
            try (LogRedirector director = new LogRedirector(logFile)) {
               doMeasurement(tests, fullResultsVersion);
            }
         } else {
            doMeasurement(tests, fullResultsVersion);
         }

      } else {
         LOG.info("Skipping measurement - result folder {} already existing", fullResultsVersion.getAbsolutePath());
      }
      final File measurementFolder = new File(fullResultsVersion, "measurements");
      return measurementFolder;
   }

   private void doMeasurement(final Set<TestCase> tests, final File fullResultsVersion) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      MeasurementConfig copied = createCopiedConfiguration();
      
      cleanTemporaryFolders();

      final AdaptiveTester tester = new AdaptiveTester(folders, copied, env);
      for (final TestCase test : tests) {
         tester.evaluate(test);
      }

      final File fullResultsFolder = folders.getFullMeasurementFolder();
      LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
      FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
   }

   private void cleanTemporaryFolders() throws IOException {
      final File fullResultsFolder = folders.getFullMeasurementFolder();
      FileUtils.deleteDirectory(fullResultsFolder);
      fullResultsFolder.mkdirs();
      folders.getDetailResultFolder().mkdirs();
      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      folders.getTempMeasurementFolder().mkdirs();
   }

   private MeasurementConfig createCopiedConfiguration() {
      MeasurementConfig copied = new MeasurementConfig(measurementConfig);
      copied.setUseKieker(false);
      copied.getExecutionConfig().setVersion(version);
      copied.getExecutionConfig().setVersionOld(versionOld);
      return copied;
   }
}
