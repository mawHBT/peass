package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.javaparser.ParseException;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependency.reader.VersionKeeper;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.GitUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GitUtils.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class DependencyViewGeneratorTest {

   private static final Logger LOG = LogManager.getLogger(ViewGeneratorIT.class);

   @Test
   public void testTwoVersions() throws IOException, InterruptedException, JAXBException, XmlPullParserException, ParseException, ViewNotFoundException {
      prepareGitUtils();

      ViewGeneratorIT.init(ViewGeneratorIT.BASIC);

      ResultsFolders resultsFolders = new ResultsFolders(ViewGeneratorIT.VIEW_IT_PROJECTFOLDER, "test");

      DependencyConfig dependencyConfig = new DependencyConfig(1, false, true);

      FakeFileIterator iteratorspied = mockIterator();

      DependencyReader reader = new DependencyReader(dependencyConfig, new PeASSFolders(TestConstants.CURRENT_FOLDER), resultsFolders,
            "", iteratorspied, new VersionKeeper(new File("/dev/null")), new ExecutionConfig(), new EnvironmentVariables());
      reader.readInitialVersion();
      reader.readDependencies();

      File expectedDiff = new File(resultsFolders.getVersionDiffFolder("000002"), "TestMe#test.txt");
      System.out.println(expectedDiff.getAbsolutePath());
      Assert.assertTrue(expectedDiff.exists());

      // TODO Test, that instrumentation sources are not added to the view

      final ExecutionData tests = Constants.OBJECTMAPPER.readValue(resultsFolders.getExecutionFile(), ExecutionData.class);
      //
      Assert.assertEquals(1, tests.getVersions().size());
      Assert.assertEquals(1, tests.getVersions().get("000002").getTests().size());
   }

   private void prepareGitUtils() throws IOException, InterruptedException {
      PowerMockito.mockStatic(GitUtils.class);
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.reset(Mockito.any());

      PowerMockito.doCallRealMethod().when(GitUtils.class);
      GitUtils.getDiff(Mockito.any(), Mockito.any());
   }

   private FakeFileIterator mockIterator() {
      List<File> versionList = Arrays.asList(ViewGeneratorIT.BASIC, ViewGeneratorIT.REPETITION);

      FakeFileIterator fakeIterator = new FakeFileIterator(TestConstants.CURRENT_FOLDER, versionList);
      fakeIterator.goToFirstCommit();
      FakeFileIterator iteratorspied = Mockito.spy(fakeIterator);
      VersionDiff fakedDiff = new VersionDiff(Arrays.asList(TestConstants.CURRENT_FOLDER), TestConstants.CURRENT_FOLDER);
      fakedDiff.addChange("src/test/java/viewtest/TestMe.java");

      Mockito.doReturn(fakedDiff)
            .when(iteratorspied)
            .getChangedClasses(Mockito.any(), Mockito.any(), Mockito.any());
      return iteratorspied;
   }
}
