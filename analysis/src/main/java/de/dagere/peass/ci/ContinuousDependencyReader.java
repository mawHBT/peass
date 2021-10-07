package de.dagere.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfiguration;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.VersionIterator;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class ContinuousDependencyReader {

   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);

   private final DependencyConfig dependencyConfig;
   private final ExecutionConfig executionConfig;
   private final KiekerConfiguration kiekerConfig;
   private final PeassFolders folders;
   private final ResultsFolders resultsFolders;
   private final EnvironmentVariables env;

   public ContinuousDependencyReader(final DependencyConfig dependencyConfig, final ExecutionConfig executionConfig, final KiekerConfiguration kiekerConfig, final PeassFolders folders,
         final ResultsFolders resultsFolders, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.executionConfig = executionConfig;
      this.kiekerConfig = new KiekerConfiguration(kiekerConfig);
      this.kiekerConfig.setUseKieker(true);
      this.kiekerConfig.setRecord(AllowedKiekerRecord.OPERATIONEXECUTION);
      this.kiekerConfig.setUseAggregation(false);
      this.folders = folders;
      this.resultsFolders = resultsFolders;
      this.env = env;
   }

   public Set<TestCase> getTests(final VersionIterator iterator, final String url, final String version, final MeasurementConfiguration measurementConfig) {
      final Dependencies dependencies = getDependencies(iterator, url);

      final Set<TestCase> tests;
      if (dependencies.getVersions().size() > 0) {
         if (dependencyConfig.isGenerateViews()) {
            tests = selectResults(version);
         } else {
            Version versionDependencies = dependencies.getVersions().get(dependencies.getNewestVersion());
            tests = versionDependencies.getTests().getTests();
         }

         // final Set<TestCase> tests = selectIncludedTests(dependencies);
         NonIncludedTestRemover.removeNotIncluded(tests, measurementConfig.getExecutionConfig());
      } else {
         tests = new HashSet<>();
         LOG.info("No test executed - version did not contain changed tests.");
      }
      return tests;
   }

   private Set<TestCase> selectResults(final String version) {
      try {
         final Set<TestCase> tests;
         if (dependencyConfig.isGenerateCoverageSelection()) {
            LOG.info("Using coverage-based test selection");
            ExecutionData executionData = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageSelectionFile(), ExecutionData.class);
            TestSet versionTestSet = executionData.getVersions().get(version);
            tests = versionTestSet != null ? versionTestSet.getTests() : new HashSet<TestCase>();
         } else {
            LOG.info("Using dynamic test selection results");
            ExecutionData executionData = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
            TestSet versionTestSet = executionData.getVersions().get(version);
            tests = versionTestSet.getTests();
         }
         return tests;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   Dependencies getDependencies(final VersionIterator iterator, final String url) {
      try {
         Dependencies dependencies;

         final VersionKeeper noChanges = new VersionKeeper(new File(resultsFolders.getDependencyFile().getParentFile(), "nonChanges_" + folders.getProjectName() + ".json"));

         if (!resultsFolders.getDependencyFile().exists()) {
            LOG.debug("Fully loading dependencies");
            dependencies = fullyLoadDependencies(url, iterator, noChanges);
         } else {
            LOG.debug("Partially loading dependencies");
            dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getDependencyFile(), Dependencies.class);
            VersionComparator.setDependencies(dependencies);
            
            if (iterator != null) {
               executePartialRTS(dependencies, iterator);
            }
         }
         VersionComparator.setDependencies(dependencies);

         return dependencies;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void executePartialRTS(final Dependencies dependencies, final VersionIterator newIterator) throws FileNotFoundException {
      if (executionConfig.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getDependencyLogFile(newIterator.getTag(), newIterator.getPredecessor());
         LOG.info("Executing regression test selection update - Log goes to {}", logFile.getAbsolutePath());
         try (LogRedirector director = new LogRedirector(logFile)) {
            doPartialRCS(dependencies, newIterator);
         }
      } else {
         doPartialRCS(dependencies, newIterator);
      }

   }

   private void doPartialRCS(final Dependencies dependencies, final VersionIterator newIterator) {
      DependencyReader reader = new DependencyReader(dependencyConfig, folders, resultsFolders, dependencies.getUrl(), newIterator,
            new VersionKeeper(new File(resultsFolders.getDependencyFile().getParentFile(), "nochanges.json")), executionConfig, kiekerConfig, env);
      newIterator.goTo0thCommit();

      reader.readCompletedVersions(dependencies);

      try {
         ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
         reader.setExecutionData(executions);

         if (resultsFolders.getCoverageSelectionFile().exists()) {
            ExecutionData coverageExecutions = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageSelectionFile(), ExecutionData.class);
            reader.setCoverageExecutions(coverageExecutions);

            if (resultsFolders.getCoverageInfoFile().exists()) {
               CoverageSelectionInfo coverageInfo = Constants.OBJECTMAPPER.readValue(resultsFolders.getCoverageInfoFile(), CoverageSelectionInfo.class);
               reader.setCoverageInfo(coverageInfo);
            }
         }

         reader.readDependencies();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   private Dependencies fullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws Exception {
      if (executionConfig.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getDependencyLogFile(iterator.getTag(), iterator.getPredecessor());
         LOG.info("Executing regression test selection - Log goes to {}", logFile.getAbsolutePath());

         try (LogRedirector director = new LogRedirector(logFile)) {
            return doFullyLoadDependencies(url, iterator, nonChanges);
         }
      } else {
         return doFullyLoadDependencies(url, iterator, nonChanges);
      }
   }

   private Dependencies doFullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws IOException, InterruptedException, XmlPullParserException, JsonParseException, JsonMappingException, ParseException, ViewNotFoundException {
      final DependencyReader reader = new DependencyReader(dependencyConfig, folders, resultsFolders, url, iterator, nonChanges, executionConfig, kiekerConfig, env);
      iterator.goToPreviousCommit();
      if (!reader.readInitialVersion()) {
         LOG.error("Analyzing first version was not possible");
      } else {
         reader.readDependencies();
      }
      Dependencies dependencies = Constants.OBJECTMAPPER.readValue(resultsFolders.getDependencyFile(), Dependencies.class);
      return dependencies;
   }
}
