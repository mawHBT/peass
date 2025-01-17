package de.dagere.peass.dependency.execution.pom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;


public class TestSnapshotRemoveUtil {
   @Test
   public void testRegularRemoval() throws FileNotFoundException, IOException, XmlPullParserException {
      File pomFile = new File("src/test/resources/example-snapshot-removal-pom/pom.xml");

      File testPom = new File("target/pom.xml");
      FileUtils.copyFile(pomFile, testPom);

      SnapshotRemoveUtil.cleanSnapshotDependencies(testPom);

      try (FileInputStream inputStream = new FileInputStream(testPom)) {
         final MavenXpp3Reader reader = new MavenXpp3Reader();
         final Model model = reader.read(inputStream);
         Dependency slf4j = findDependency(model, "slf4j");
         // Currently, if self groupId is null, all dependencies stay the same
         Assert.assertEquals("1.0-SNAPSHOT", slf4j.getVersion());
      }
   }

   public static Dependency findDependency(final Model model, final String artifactPart) {
      Dependency slf4j = null;
      for (Dependency dependency : model.getDependencies()) {
         
         if (dependency.getArtifactId().contains(artifactPart)) {
            slf4j = dependency;
            
         }
      }
      return slf4j;
   }
}
