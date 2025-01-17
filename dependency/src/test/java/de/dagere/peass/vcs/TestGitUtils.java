package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;

public class TestGitUtils {

   private static final String FEATURE_A = "feature-A";
   private static final String PEASS_TEST_MAIN_BRANCH = "peass-test-main";
   private final static File PROJECT_FOLDER = new File(TestConstants.CURRENT_FOLDER, "demo-git");

   private static final File exampleTextFile = new File(PROJECT_FOLDER, "file.txt");

   @BeforeEach
   public void prepareProject() throws InterruptedException, IOException {
      TestUtil.deleteContents(PROJECT_FOLDER);
      PROJECT_FOLDER.mkdirs();
      ProjectBuilderHelper.init(PROJECT_FOLDER);

      FileUtils.writeStringToFile(exampleTextFile, "Dummy", StandardCharsets.UTF_8);
      ProjectBuilderHelper.commit(PROJECT_FOLDER, "Dummy-version for avoiding branch clashes (default branch might be main or master)");

      // Use main branch, regardless of what the system has configured as default branch (e.g. might be master)
      ProjectBuilderHelper.branch(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);

      for (int i = 0; i < 3; i++) {
         createCommit(exampleTextFile, "", i);
      }

      ProjectBuilderHelper.branch(PROJECT_FOLDER, FEATURE_A);
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, FEATURE_A);

      for (int i = 0; i < 3; i++) {
         createCommit(exampleTextFile, "A", i);
      }
      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
   }

   private void createCommit(final File exampleTextFile, final String prefix, final int i) throws IOException, InterruptedException {
      FileUtils.writeStringToFile(exampleTextFile, prefix + i, StandardCharsets.UTF_8);
      ProjectBuilderHelper.commit(PROJECT_FOLDER, "Version " + prefix + i);
   }

   @Test
   public void testBasicCommitGetting() throws InterruptedException, IOException {
      List<GitCommit> commitsAll = GitUtils.getCommits(PROJECT_FOLDER, true, false, true);
      Assert.assertEquals(commitsAll.size(), 7);

      List<GitCommit> commits = GitUtils.getCommits(PROJECT_FOLDER, false, false, true);
      Assert.assertEquals(commits.size(), 4);

      ProjectBuilderHelper.merge(PROJECT_FOLDER, FEATURE_A);

      List<GitCommit> commitsMerged = GitUtils.getCommits(PROJECT_FOLDER, false, false, true);
      Assert.assertEquals(commitsMerged.size(), 7);

      MatcherAssert.assertThat(commitsMerged.get(0).getComitter(), Matchers.equalTo("Anonym <anonym@generated.org>"));
      Assert.assertNotNull(commitsMerged.get(0).getDate());

      MatcherAssert.assertThat(commitsMerged.get(commitsMerged.size() - 1).getMessage(), Matchers.containsString("Version A2"));
      MatcherAssert.assertThat(commitsMerged.get(0).getMessage(), Matchers.containsString("Dummy-version for avoiding branch clashes"));
   }

   @Test
   public void testMergeCommits() throws InterruptedException, IOException {
      createMergeCommit(exampleTextFile, 3);
      createMergeCommit(exampleTextFile, 4);

      List<GitCommit> commitsRegular = GitUtils.getCommits(PROJECT_FOLDER, false, false, false);
      Assert.assertEquals(commitsRegular.size(), 13);

      // Current linearization produces 7, 8, 10 or 11 commits based on hashes; every of these linearizations is ok (it just should not be 13)
      List<GitCommit> commitsLinear = GitUtils.getCommits(PROJECT_FOLDER, false, true, false);
      MatcherAssert.assertThat(commitsLinear.size(), Matchers.anyOf(Matchers.is(7), Matchers.is(8), Matchers.is(10), Matchers.is(11)));
   }

   private void createMergeCommit(final File exampleTextFile, final int index) throws InterruptedException, IOException {
      createCommit(exampleTextFile, "", index);

      ProjectBuilderHelper.checkout(PROJECT_FOLDER, FEATURE_A);
      createCommit(exampleTextFile, "A", index);

      ProjectBuilderHelper.checkout(PROJECT_FOLDER, PEASS_TEST_MAIN_BRANCH);
      ProjectBuilderHelper.mergeTheirs(PROJECT_FOLDER, FEATURE_A);
   }
}
