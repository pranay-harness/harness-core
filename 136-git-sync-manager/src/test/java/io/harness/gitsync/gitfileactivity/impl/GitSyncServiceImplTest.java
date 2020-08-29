package io.harness.gitsync.gitfileactivity.impl;

import static io.harness.gitsync.core.beans.GitCommit.Status.COMPLETED;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.TriggeredBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;

public class GitSyncServiceImplTest extends GitSyncBaseTest {
  @Inject @Spy private GitSyncServiceImpl gitSyncService;

  private String accountId = "accountId";
  private String gitConnectorId = "gitConnectorId";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_shouldLogActivityForGitOperation() {
    final String commitId = "commitId";
    final String filePath = "rootpath/connector/file1.yaml";
    final String commitMessage = "commitMessage";
    Iterable<GitFileActivity> gitFileActivities =
        gitSyncService.logActivityForGitOperation(Collections.singletonList(GitFileChange.builder()
                                                                                .filePath(filePath)
                                                                                .commitMessage(commitMessage)
                                                                                .accountId(accountId)
                                                                                .commitId(commitId)
                                                                                .changeFromAnotherCommit(Boolean.TRUE)
                                                                                .build()),
            GitFileActivity.Status.SUCCESS, false, false, accountId, commitId, commitMessage,
            YamlGitConfigDTO.builder().branch("branchName").gitConnectorId(gitConnectorId).build());

    GitFileActivity fileActivity = gitFileActivities.iterator().next();

    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(GitFileActivity.Status.SUCCESS);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivity.getProcessingCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getProcessingCommitMessage()).isEqualTo(commitMessage);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_shouldCreateGitFileActivitySummary() {
    String commitId = "commitId";
    String commitMessage = "commitMessage";
    String filePath1 = "filePath1";
    String branchName = "branchName";
    doReturn(Collections.singletonList(GitFileActivity.builder()
                                           .accountId(accountId)
                                           .triggeredBy(TriggeredBy.FULL_SYNC)
                                           .commitId(commitId)
                                           .processingCommitId(commitId)
                                           .filePath(filePath1)
                                           .gitConnectorId(gitConnectorId)
                                           .branchName(branchName)
                                           .commitMessage(commitMessage)
                                           .status(GitFileActivity.Status.SUCCESS)
                                           .build()))
        .when(gitSyncService)
        .getFileActivitesForCommit(anyString(), anyString());

    GitFileActivitySummary fileActivitySummary =
        gitSyncService.createGitFileActivitySummaryForCommit(commitId, accountId, false, COMPLETED);

    assertThat(fileActivitySummary.getAccountId()).isEqualTo(accountId);
    assertThat(fileActivitySummary.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivitySummary.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(fileActivitySummary.getBranchName()).isEqualTo(branchName);
    assertThat(fileActivitySummary.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivitySummary.getStatus()).isEqualTo(COMPLETED);
    assertThat(fileActivitySummary.getGitToHarness()).isEqualTo(false);
    assertThat(fileActivitySummary.getFileProcessingSummary().getSuccessCount()).isEqualTo(1);
    assertThat(fileActivitySummary.getFileProcessingSummary().getTotalCount()).isEqualTo(1);
  }
}