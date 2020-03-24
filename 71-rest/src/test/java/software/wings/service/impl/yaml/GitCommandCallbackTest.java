package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.GitCommit;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitWebhookRequestAttributes;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitCommandCallbackTest extends CategoryTest {
  private static final String CHANGESET_ID = "changesetId";

  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private YamlGitService yamlGitService;
  @InjectMocks
  private GitCommandCallback commandCallback =
      new GitCommandCallback(ACCOUNT_ID, CHANGESET_ID, GitCommandType.COMMIT_AND_PUSH);

  @InjectMocks
  @Spy
  private GitCommandCallback diffCommandCallback =
      new GitCommandCallback(ACCOUNT_ID, CHANGESET_ID, GitCommandType.DIFF);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCallbackForGitConnectionFailure() throws Exception {
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
                                          .errorCode(ErrorCode.GIT_CONNECTION_ERROR)
                                          .gitCommandStatus(GitCommandStatus.FAILURE)
                                          .errorMessage("cant connect ot git")
                                          .build();

    doReturn(true).when(yamlChangeSetService).updateStatus(anyString(), anyString(), any());
    doNothing().when(yamlGitService).raiseAlertForGitFailure(anyString(), anyString(), any(), anyString());
    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    commandCallback.notify(map);
    verify(yamlGitService, times(1)).raiseAlertForGitFailure(anyString(), anyString(), any(), anyString());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCallbackForGitConnectionSuccess() throws Exception {
    ResponseData notifyResponseData =
        GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();

    doReturn(true).when(yamlChangeSetService).updateStatus(anyString(), anyString(), any());
    doThrow(new RuntimeException())
        .when(yamlGitService)
        .closeAlertForGitFailureIfOpen(anyString(), anyString(), any(), any());
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotifyOnErrorCase() {
    ResponseData notifyResponseData = ErrorNotifyResponseData.builder().build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    commandCallback.notify(map);
    verify(yamlGitService, never())
        .closeAlertForGitFailureIfOpen(
            anyString(), anyString(), any(AlertType.class), any(GitConnectionErrorAlert.class));
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, Status.FAILED);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotifyWithUnhandledGitCommandType() {
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
                                          .gitCommandStatus(GitCommandStatus.SUCCESS)
                                          .gitCommandResult(GitCheckoutResult.builder().build())
                                          .build();

    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    on(commandCallback).set("gitCommandType", GitCommandType.CHECKOUT);
    commandCallback.notify(map);
    verify(yamlGitService, times(1))
        .closeAlertForGitFailureIfOpen(ACCOUNT_ID, GLOBAL_APP_ID, AlertType.GitConnectionError,
            GitConnectionErrorAlert.builder().accountId(ACCOUNT_ID).build());
    verify(yamlChangeSetService, times(1)).updateStatus(ACCOUNT_ID, CHANGESET_ID, Status.FAILED);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_addActiveGitSyncErrorsToProcessAgain() {
    final YamlGitConfig yamlGitConfig =
        YamlGitConfig.builder().gitConnectorId("gitconnectorid").branchName("branchname").build();
    final GitFileChange gitFileChange1 = aGitFileChange()
                                             .withFilePath("Setup/Applications/app1/index.yaml")
                                             .withFileContent("error_content")
                                             .withCommitId("commitid-1")
                                             .withSyncFromGit(false)
                                             .withAccountId(ACCOUNT_ID)
                                             .withChangeType(ChangeType.DELETE)
                                             .withYamlGitConfig(yamlGitConfig)
                                             .build();
    final GitFileChange gitFileChange2 = aGitFileChange()
                                             .withFilePath("Setup/Applications/app2/index.yaml")
                                             .withFileContent("error_content")
                                             .withCommitId("commitid-1")
                                             .withSyncFromGit(false)
                                             .withAccountId(ACCOUNT_ID)
                                             .withChangeType(ChangeType.DELETE)
                                             .withYamlGitConfig(yamlGitConfig)
                                             .build();
    final GitDiffResult gitDiffResult =
        GitDiffResult.builder().yamlGitConfig(yamlGitConfig).gitFileChanges(Lists.newArrayList()).build();
    gitDiffResult.addChangeFile(gitFileChange1);
    gitDiffResult.addChangeFile(gitFileChange2);
    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .yamlFilePath("Setup/index.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .build();
    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .yamlFilePath("Setup/Applications/app1/index.yaml")
                                           .yamlContent("ds")
                                           .accountId(ACCOUNT_ID)
                                           .changeType("MODIFY")
                                           .fullSyncPath(false)
                                           .gitCommitId("commitid")
                                           .build();
    doReturn(Arrays.asList(gitSyncError1, gitSyncError2))
        .when(yamlGitService)
        .getActiveGitToHarnessSyncErrors(eq(ACCOUNT_ID), eq("gitconnectorid"), eq("branchname"), anyLong());

    diffCommandCallback.addActiveGitSyncErrorsToProcessAgain(gitDiffResult, ACCOUNT_ID);
    final List<GitFileChange> modifiedGitFileChangeSetList = gitDiffResult.getGitFileChanges();
    assertThat(modifiedGitFileChangeSetList.size()).isEqualTo(3);
    assertThat(modifiedGitFileChangeSetList).contains(gitFileChange1);
    assertThat(modifiedGitFileChangeSetList).contains(gitFileChange2);
    assertThat(modifiedGitFileChangeSetList).contains(gitFileChange2);
    final GitFileChange gitFileChange3 =
        modifiedGitFileChangeSetList.stream()
            .filter(gitFileChange -> gitFileChange != gitFileChange1 && gitFileChange != gitFileChange2)
            .findFirst()
            .get();
    assertThat(gitFileChange3.getFilePath().equals(gitSyncError1.getYamlFilePath())).isTrue();
    assertThat(gitFileChange3.getFileContent().equals(gitSyncError1.getYamlContent())).isTrue();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testCallbackForGitDiffFailure() throws Exception {
    ResponseData notifyResponseData = GitCommandExecutionResponse.builder()
                                          .errorCode(ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER)
                                          .gitCommandStatus(GitCommandStatus.FAILURE)
                                          .errorMessage("cant connect ot git")
                                          .build();

    doReturn(true).when(yamlChangeSetService).updateStatus(anyString(), anyString(), any());
    final GitWebhookRequestAttributes webhookRequestAttributes = GitWebhookRequestAttributes.builder()
                                                                     .headCommitId("head")
                                                                     .branchName("master")
                                                                     .gitConnectorId("gitconnector")
                                                                     .build();
    final YamlChangeSet yamlChangeSet =
        YamlChangeSet.builder().gitWebhookRequestAttributes(webhookRequestAttributes).build();

    doReturn(yamlChangeSet).when(yamlChangeSetService).get(ACCOUNT_ID, CHANGESET_ID);
    doReturn(ImmutableList.of("yamlgitconfig1", "yamlgitconfig2"))
        .when(diffCommandCallback)
        .obtainYamlGitConfigIds(ACCOUNT_ID, "master", "gitconnector");
    doReturn(GitCommit.builder().build()).when(yamlGitService).saveCommit(any(GitCommit.class));
    Map<String, ResponseData> map = new HashMap<>();
    map.put("key", notifyResponseData);

    diffCommandCallback.notify(map);
    verify(yamlGitService, times(1)).saveCommit(any(GitCommit.class));
  }
}
