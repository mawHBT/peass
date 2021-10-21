package net.kieker.sourceinstrumentation.it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.utils.StreamGobbler;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.JavaVersionUtil;
import net.kieker.sourceinstrumentation.SourceInstrumentationTestUtil;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class SourceInstrumentationEnumIT {

   @BeforeEach
   public void before() throws IOException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
   }

   public File writeAdaptiveInstrumentationInfo() throws IOException {
      final File configFolder = new File(TestConstants.CURRENT_FOLDER, "config");
      configFolder.mkdir();

      final File adaptiveFile = new File(TestConstants.CURRENT_FOLDER, MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(adaptiveFile))) {
         writer.write("- *\n");
         writer.write("+ public void de.peass.MainTest.testMe()\n");
         writer.flush();
      }
      return adaptiveFile;
   }

   @Test
   public void testExecution() throws IOException {
      SourceInstrumentationTestUtil.initSimpleProject("/sourceInstrumentation/example_enum/");

      File tempFolder = new File(TestConstants.CURRENT_FOLDER, "results");
      tempFolder.mkdir();

      InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.DURATION, true, null, false, false, 10, false);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(configuration);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      final ProcessBuilder pb = new ProcessBuilder("mvn", "test",
            "-Djava.io.tmpdir=" + tempFolder.getAbsolutePath());
      pb.directory(TestConstants.CURRENT_FOLDER);

      Process process = pb.start();
      StreamGobbler.showFullProcess(process);

      File resultFolder = tempFolder.listFiles()[0];
      File resultFile = resultFolder.listFiles((FileFilter) new WildcardFileFilter("*.dat"))[0];

      String monitorLogs = FileUtils.readFileToString(resultFile, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(monitorLogs, Matchers.containsString("public void de.peass.MainTest.testMe()"));
      if (JavaVersionUtil.getSystemJavaVersion() > 8) {
         MatcherAssert.assertThat(monitorLogs, Matchers.not(Matchers.containsString("public void de.peass.AddRandomNumbers.addSomething();")));
      }
   }
}