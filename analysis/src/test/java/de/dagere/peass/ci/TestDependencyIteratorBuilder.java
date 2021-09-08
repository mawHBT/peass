package de.dagere.peass.ci;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIteratorGit;

public class TestDependencyIteratorBuilder {

   private static final File TEMPORARY_FOLDER = new File("target/temp");
   private static final String SIMPLE_PREDECESSOR = "000001";
   private static final String LAST_RUNNING_VERSION = "00000A";
   private static final String VERSION_2 = "000002";

   @Test
   public void testRegularIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         ExecutionConfig config = new ExecutionConfig();
         config.setVersionOld(SIMPLE_PREDECESSOR);
         config.setVersion("HEAD");

         Dependencies dependencies = buildVersionDependencies(LAST_RUNNING_VERSION);
         
         VersionIteratorGit iterator = new DependencyIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getTag());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getPredecessor());
      }
   }
   
   @Test
   public void testNightlyBuildWithoutPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         ExecutionConfig config = new ExecutionConfig();
         config.setVersionOld(null);
         config.setVersion("HEAD");

         Dependencies dependencies = buildVersionDependencies();
         
         DependencyIteratorBuilder builder = new DependencyIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         VersionIteratorGit iterator = builder.getIterator();
         Assert.assertNull(iterator);
         Assert.assertEquals(VERSION_2, builder.getVersion());
         Assert.assertEquals(null, builder.getVersionOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         ExecutionConfig config = new ExecutionConfig();
         config.setVersionOld(null);
         config.setVersion("HEAD");

         Dependencies dependencies = buildVersionDependencies(SIMPLE_PREDECESSOR, VERSION_2);
         
         DependencyIteratorBuilder builder = new DependencyIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         VersionIteratorGit iterator = builder.getIterator();
         Assert.assertNull(iterator);
         Assert.assertEquals(VERSION_2, builder.getVersion());
         Assert.assertEquals(SIMPLE_PREDECESSOR, builder.getVersionOld());
      }
   }

   @Test
   public void testNightlyBuildIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         ExecutionConfig config = new ExecutionConfig();
         config.setVersionOld(null);
         config.setVersion("HEAD");
         
         Dependencies dependencies = buildVersionDependencies(LAST_RUNNING_VERSION);

         VersionIteratorGit iterator = new DependencyIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getTag());
         Assert.assertEquals(LAST_RUNNING_VERSION, iterator.getPredecessor());
      }
   }

   @Test
   public void testInitialRun() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName("HEAD", TEMPORARY_FOLDER)).thenReturn(VERSION_2);
         gitUtil.when(() -> GitUtils.getName("HEAD~1", TEMPORARY_FOLDER)).thenReturn(SIMPLE_PREDECESSOR);
         
         ExecutionConfig config = new ExecutionConfig();
         config.setVersionOld(null);
         config.setVersion("HEAD");

         VersionIteratorGit iterator = new DependencyIteratorBuilder(config, null, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getTag());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getPredecessor());
      }
   }
   
   private Dependencies buildVersionDependencies(final String... versionNames) {
      Dependencies dependencies = new Dependencies();
      for (String versionName : versionNames) {
         Version version = new Version();
         version.setRunning(true);
         dependencies.getVersions().put(versionName, version);
      }
      
      return dependencies;
   }
}
