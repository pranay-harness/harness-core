package io.harness.gitsync.core.callback;

import static io.harness.rule.OwnerRule.ABHINAV;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eraro.ErrorCode;
import io.harness.git.model.ChangeType;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.CommitResult;
import io.harness.git.model.DiffResult;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.gitsync.core.impl.GitChangeSetProcessor;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class GitCommandCallbackTest extends CategoryTest {
  private static final String CHANGESET_ID = "changesetId";
  private static final String gitConnectorId = "gitConnectorId";
  private static final String repositoryName = "repositoryName";
  private static final String branchName = "branchName";
  private final YamlGitConfigDTO yamlGitConfig = YamlGitConfigDTO.builder().build();
  @Mock private YamlGitService yamlGitService;
  @Mock private GitSyncService gitSyncService;
  @Mock private GitCommitService gitCommitService;
  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private GitChangeSetProcessor gitChangeSetProcessor;
  @Mock private GitSyncErrorService gitSyncErrorService;

  @InjectMocks
  private GitCommandCallback commandCallback = new GitCommandCallback(ACCOUNT_ID, CHANGESET_ID,
      GitCommandType.COMMIT_AND_PUSH, gitConnectorId, repositoryName, branchName, yamlGitConfig);

  @InjectMocks
  @Spy
  private GitCommandCallback commandCallbackDiff = new GitCommandCallback(
      ACCOUNT_ID, CHANGESET_ID, GitCommandType.DIFF, gitConnectorId, repositoryName, branchName, yamlGitConfig);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCallbackForGitConnectionFailure() {
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .errorCode(ErrorCode.GIT_CONNECTION_ERROR)
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.FAILURE)
            .errorMessage("cant connect ot git")
            .build();

    doReturn(true).when(yamlChangeSetService).updateStatus(anyString(), anyString(), any());
    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    commandCallback.notify(map);
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCallbackForGitConnectionSuccess() throws Exception {
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
            .build();

    doReturn(true).when(yamlChangeSetService).updateStatus(anyString(), anyString(), any());

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    try {
      commandCallback.notify(map);
      assertThat(false).isTrue();
    } catch (RuntimeException e) {
      // the way we are testing here is, for GitCommandStatus.SUCCESS, closeAlertForGitFailureIfOpen() should get
      // called. in mock, we are throwing exception on this method call as this is what we want to test. rest of code
      // flow is not required for this unit test
      assertThat(true).isTrue();
    }
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyOnErrorCase() {
    DelegateResponseData notifyResponseData = ErrorNotifyResponseData.builder().build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    commandCallback.notify(map);
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyWithUnhandledGitCommandType() {
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
            .gitCommandResult(CommitResult.builder().build())
            .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    on(commandCallback).set("gitCommandType", GitCommandType.CHECKOUT);
    commandCallback.notify(map);

    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_getAllFilesSuccessFullyProccessed() {
    GitFileChange gitFileChange1 = GitFileChange.builder().filePath("a").build();
    GitFileChange gitFileChange2 = GitFileChange.builder().filePath("a").build();
    GitFileChange gitFileChange3 = GitFileChange.builder().filePath("b").build();
    GitFileChange gitFileChange4 = GitFileChange.builder().filePath("c").build();

    List<GitFileChange> fileChangesPartOfYamlChangeSet = Arrays.asList(gitFileChange1, gitFileChange3);
    List<GitFileChange> filesCommited = Arrays.asList(gitFileChange2, gitFileChange4);
    List<GitFileChange> allFilesProcessed =
        commandCallback.getAllFilesSuccessFullyProccessed(fileChangesPartOfYamlChangeSet, filesCommited);
    assertThat(allFilesProcessed.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyForGitCommitAndPush() {
    io.harness.git.model.GitFileChange gitFileChange = GitFileChange.builder().changeType(ChangeType.ADD).build();
    String commitId = "commitId";
    CommitResult gitCommitResult = CommitResult.builder().commitId(commitId).build();
    String yamlChangeSetId = "yamlChangeSetId";
    CommitAndPushResult gitCommitAndPushResult = CommitAndPushResult.builder()
                                                     .filesCommittedToGit(Arrays.asList(gitFileChange))
                                                     .gitCommitResult(gitCommitResult)
                                                     .build();
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
            .gitCommandResult(gitCommitAndPushResult)
            .gitCommandRequest(CommitAndPushRequest.builder().build())

            .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    doReturn(Optional.of(YamlChangeSet.builder().gitFileChanges(Collections.singletonList(gitFileChange)).build()))
        .when(yamlChangeSetService)
        .get(any(), any());
    GitCommit gitCommit = GitCommit.builder()
                              .accountId(ACCOUNT_ID)
                              .commitId(commitId)
                              .status(GitCommit.Status.COMPLETED)
                              .yamlChangeSetId(yamlChangeSetId)
                              .build();
    doReturn(gitCommit).when(gitCommitService).save(any());
    on(commandCallback).set("gitCommandType", GitCommandType.COMMIT_AND_PUSH);
    commandCallback.notify(map);

    verify(yamlGitService, times(1))
        .removeGitSyncErrors(ACCOUNT_ID, null, null, Collections.singletonList(gitFileChange), false);

    verify(gitSyncService, times(1))
        .logActivityForGitOperation(Collections.singletonList(gitFileChange), GitFileActivity.Status.SUCCESS, false,
            false, "", commitId, null, yamlGitConfig);
    verify(gitSyncService, times(1))
        .createGitFileActivitySummaryForCommit(commitId, ACCOUNT_ID, false, GitCommit.Status.COMPLETED, yamlGitConfig);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyForDiffForFailure() {
    io.harness.git.model.GitFileChange gitFileChange = GitFileChange.builder().changeType(ChangeType.ADD).build();
    String commitId = "commitId";
    DiffResult diffResult = DiffResult.builder().gitFileChanges(Collections.singletonList(gitFileChange)).build();
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.FAILURE)
            .gitCommandResult(diffResult)
            .gitCommandRequest(CommitAndPushRequest.builder().build())

            .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    doReturn(Optional.of(YamlChangeSet.builder()
                             .gitWebhookRequestAttributes(GitWebhookRequestAttributes.builder()
                                                              .branchName(branchName)
                                                              .gitConnectorId(gitConnectorId)
                                                              .repo(repositoryName)
                                                              .headCommitId("HEAD")
                                                              .build())
                             .build()))
        .when(yamlChangeSetService)
        .get(any(), any());
    GitCommit gitCommit =
        GitCommit.builder().accountId(ACCOUNT_ID).commitId(commitId).status(GitCommit.Status.COMPLETED).build();
    doReturn(gitCommit).when(gitCommitService).save(any());
    doReturn(true).when(yamlChangeSetService).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
    doNothing()
        .when(gitChangeSetProcessor)
        .processGitChangeSet(anyString(), any(), anyString(), anyString(), anyString());
    doNothing().when(gitSyncService).createGitFileSummaryForFailedOrSkippedCommit(eq(any()), true);

    on(commandCallbackDiff).set("gitCommandType", GitCommandType.DIFF);
    commandCallbackDiff.notify(map);

    verify(gitCommitService, times(1)).save(any());
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyForDiff() {
    GitFileChange gitFileChange = GitFileChange.builder().changeType(ChangeType.ADD).build();
    String commitId = "commitId";
    DiffResult diffResult =
        DiffResult.builder().gitFileChanges(new ArrayList<>(Collections.singletonList(gitFileChange))).build();
    DelegateResponseData notifyResponseData =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
            .gitCommandResult(diffResult)
            .gitCommandRequest(CommitAndPushRequest.builder().build())

            .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);
    final GitSyncError gitSyncError = GitSyncError.builder().yamlFilePath("filePath").build();
    doReturn(Collections.singletonList(gitSyncError))
        .when(gitSyncErrorService)
        .getActiveGitToHarnessSyncErrors(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());

    on(commandCallbackDiff).set("gitCommandType", GitCommandType.DIFF);
    commandCallbackDiff.notify(map);

    doNothing()
        .when(gitChangeSetProcessor)
        .processGitChangeSet(anyString(), any(), anyString(), anyString(), anyString());
    verify(gitChangeSetProcessor, times(1)).processGitChangeSet(any(), any(), any(), any(), any());
  }
}
