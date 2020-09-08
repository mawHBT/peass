package de.peass.dependencytests;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Assert;
import org.mockito.Mockito;

import de.peass.dependency.ChangeManager;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;

public class DependencyDetectorTestUtil {
   static TestSet findDependency(final Dependencies dependencies, final String changedClass, final String version) {
      final Version versionDependencies = dependencies.getVersions().get(version);
      System.out.println(dependencies.getVersions().keySet());
      Assert.assertNotNull("Searching for " + changedClass + " in " + version, versionDependencies);

      TestSet testcase = null;
      for (final Entry<ChangedEntity, TestSet> candidate : versionDependencies.getChangedClazzes().entrySet()) {
         final String changeclassInDependencies = candidate.getKey().toString();
         if (changeclassInDependencies.equals(changedClass)) {
            testcase = candidate.getValue();
         }
      }
      return testcase;
   }
   
   public static Map<ChangedEntity, ClazzChangeData> buildChanges(final String module, final String clazz, final String method) {
      final Map<ChangedEntity, ClazzChangeData> changes = new TreeMap<>();
      addChange(changes, module, clazz, method);
      return changes;
   }

   public static void addChange(final Map<ChangedEntity, ClazzChangeData> changes, final String module, final String clazz, final String method) {
      final ChangedEntity baseChangedClazz = new ChangedEntity(clazz, module);
      final ClazzChangeData methodChanges = new ClazzChangeData(baseChangedClazz);
      methodChanges.addChange(clazz.substring(clazz.lastIndexOf('.') + 1), method);
      changes.put(baseChangedClazz, methodChanges);
   }
   
   public static ChangeManager defaultChangeManager() {
      final Map<ChangedEntity, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("", "defaultpackage.NormalDependency", "executeThing");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }
}
