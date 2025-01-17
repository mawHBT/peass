package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.execution.PomJavaUpdater;
import de.dagere.peass.dependency.execution.ProjectModules;
import de.dagere.peass.dependency.execution.pom.SnapshotRemoveUtil;
import de.dagere.peass.folders.PeassFolders;

public class MavenUpdater {
   
   private static final Logger LOG = LogManager.getLogger(MavenUpdater.class);
   
   private final PeassFolders folders;
   private final ProjectModules modules;
   private final MeasurementConfig measurementConfig;
   
   public MavenUpdater(final PeassFolders folders, final ProjectModules modules, final MeasurementConfig measurementConfig) {
      this.folders = folders;
      this.modules = modules;
      this.measurementConfig = measurementConfig;
   }

   public void updateJava() throws FileNotFoundException, IOException, XmlPullParserException {
      final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      if (measurementConfig.getExecutionConfig().isRemoveSnapshots()) {
         SnapshotRemoveUtil.cleanSnapshotDependencies(pomFile);
      }
      PomJavaUpdater.fixCompilerVersion(pomFile);
      for (File module : modules.getModules()) {
         final File pomFileModule = new File(module, "pom.xml");
         if (measurementConfig.getExecutionConfig().isRemoveSnapshots()) {
            SnapshotRemoveUtil.cleanSnapshotDependencies(pomFileModule);
         }
         PomJavaUpdater.fixCompilerVersion(pomFileModule);
      }
   }
}
