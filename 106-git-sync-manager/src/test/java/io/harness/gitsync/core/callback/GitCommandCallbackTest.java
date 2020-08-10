package io.harness.gitsync.core.callback;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.git.GitCheckoutResult;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.beans.git.GitCommitResult;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eraro.ErrorCode;
import io.harness.gitsync.common.CommonsMapper;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GitCommandCallbackTest extends CategoryTest {
  private static final String CHANGESET_ID = "changesetId";
  private static final String gitConnectorId = "gitConnectorId";
  private static final String repositoryName = "repositoryName";
  private static final String branchName = "branchName";

  @Mock private YamlGitService yamlGitService;
  @Mock private GitSyncService gitSyncService;
  @Mock private GitCommitService gitCommitService;
  @Mock private YamlChangeSetService yamlChangeSetService;

  @InjectMocks
  private GitCommandCallback commandCallback = new GitCommandCallback(
      ACCOUNT_ID, CHANGESET_ID, GitCommandType.COMMIT_AND_PUSH, gitConnectorId, repositoryName, branchName);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCallbackForGitConnectionFailure() {
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
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
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
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
    ResponseData notifyResponseData = ErrorNotifyResponseData.builder().build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    commandCallback.notify(map);
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, YamlChangeSet.Status.FAILED);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testNotifyWithUnhandledGitCommandType() {
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
                                          .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
                                          .gitCommandResult(GitCheckoutResult.builder().build())
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
    io.harness.delegate.beans.git.GitFileChange gitFileChange =
        io.harness.delegate.beans.git.GitFileChange.builder()
            .changeType(io.harness.delegate.beans.git.GitFileChange.ChangeType.ADD)
            .build();
    String commitId = "commitId";
    GitCommitResult gitCommitResult = GitCommitResult.builder().commitId(commitId).build();
    String yamlChangeSetId = "yamlChangeSetId";
    GitCommitAndPushResult gitCommitAndPushResult = GitCommitAndPushResult.builder()
                                                        .filesCommitedToGit(Arrays.asList(gitFileChange))
                                                        .gitCommitResult(gitCommitResult)
                                                        .yamlGitConfig(YamlGitConfigDTO.builder().build())
                                                        .build();
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
                                          .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS)
                                          .gitCommandResult(gitCommitAndPushResult)
                                          .gitCommandRequest(GitCommitAndPushRequest.builder()
                                                                 .yamlChangeSetId(yamlChangeSetId)
                                                                 .yamlGitConfigs(YamlGitConfigDTO.builder().build())
                                                                 .build())

                                          .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    doReturn(
        Optional.of(YamlChangeSet.builder()
                        .gitFileChanges(Collections.singletonList(CommonsMapper.toCoreGitFileChange(gitFileChange)))
                        .build()))
        .when(yamlChangeSetService)
        .get(any(), any());
    GitCommit gitCommit = GitCommit.builder()
                              .accountId(ACCOUNT_ID)
                              .commitId(commitId)
                              .gitCommandResult(gitCommitAndPushResult)
                              .status(GitCommit.Status.COMPLETED)
                              .yamlChangeSetId(yamlChangeSetId)
                              .build();
    doReturn(gitCommit).when(gitCommitService).save(any());
    on(commandCallback).set("gitCommandType", GitCommandType.COMMIT_AND_PUSH);
    commandCallback.notify(map);

    verify(yamlGitService, times(1))
        .removeGitSyncErrors(
            ACCOUNT_ID, null, null, Collections.singletonList(CommonsMapper.toCoreGitFileChange(gitFileChange)), false);

    verify(gitSyncService, times(1))
        .logActivityForGitOperation(Collections.singletonList(CommonsMapper.toCoreGitFileChange(gitFileChange)),
            GitFileActivity.Status.SUCCESS, false, false, "", commitId, null);
    verify(gitSyncService, times(1))
        .createGitFileActivitySummaryForCommit(commitId, ACCOUNT_ID, false, GitCommit.Status.COMPLETED);
  }
}
