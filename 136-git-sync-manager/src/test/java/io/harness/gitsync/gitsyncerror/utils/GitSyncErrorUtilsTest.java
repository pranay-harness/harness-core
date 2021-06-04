package io.harness.gitsync.gitsyncerror.utils;

import static io.harness.gitsync.common.beans.GitSyncDirection.GIT_TO_HARNESS;
import static io.harness.gitsync.common.beans.GitSyncDirection.HARNESS_TO_GIT;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitSyncErrorUtilsTest extends CategoryTest {
  public static final String EMPTY_STR = "";

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testIsGitToHarnessSyncError() {
    GitSyncError gitToHarnessError = GitSyncError.builder().gitSyncDirection(GIT_TO_HARNESS).build();
    boolean result = GitSyncErrorUtils.isGitToHarnessSyncError(gitToHarnessError);
    assertThat(result).isTrue();

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection(HARNESS_TO_GIT).build();
    boolean resuultedExcpectedToBeFalse = GitSyncErrorUtils.isGitToHarnessSyncError(harnessToGitError);
    assertThat(resuultedExcpectedToBeFalse).isFalse();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetCommitIdOfError() {
    String gitCommitId = "gitCommitId";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId(gitCommitId).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection(GIT_TO_HARNESS)
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String outputCommitId = GitSyncErrorUtils.getCommitIdOfError(gitToHarnessError);
    assertThat(outputCommitId).isEqualTo(gitCommitId);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection(HARNESS_TO_GIT).build();
    String commitIdOfHarnessToGit = GitSyncErrorUtils.getCommitIdOfError(harnessToGitError);
    assertThat(commitIdOfHarnessToGit).isEqualTo(EMPTY_STR);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testgetYamlContentOfError() {
    String yamlContent = "yamlContent";
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().yamlContent(yamlContent).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection(GIT_TO_HARNESS)
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    String yamlContentOfGitToHarness = GitSyncErrorUtils.getYamlContentOfError(gitToHarnessError);
    assertThat(yamlContentOfGitToHarness).isEqualTo(yamlContent);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection(HARNESS_TO_GIT).build();
    String YamlContentOfHarnessToGit = GitSyncErrorUtils.getCommitIdOfError(harnessToGitError);
    assertThat(YamlContentOfHarnessToGit).isEqualTo(EMPTY_STR);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testgetCommitTimeOfError() {
    Long commitTime = 123L;
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().commitTime(commitTime).build();
    GitSyncError gitToHarnessError = GitSyncError.builder()
                                         .gitSyncDirection(GIT_TO_HARNESS)
                                         .additionalErrorDetails(gitToHarnessErrorDetails)
                                         .build();
    Long commitTimeOfGitToHarness = GitSyncErrorUtils.getCommitTimeOfError(gitToHarnessError);
    assertThat(commitTimeOfGitToHarness).isEqualTo(commitTime);

    GitSyncError harnessToGitError = GitSyncError.builder().gitSyncDirection(HARNESS_TO_GIT).build();
    Long commitTimeOfHarnessToGit = GitSyncErrorUtils.getCommitTimeOfError(harnessToGitError);
    assertThat(commitTimeOfHarnessToGit).isEqualTo(0L);
  }
}
