package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

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
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.HashMap;
import java.util.Map;

public class GitCommandCallbackTest extends CategoryTest {
  private static final String CHANGESET_ID = "changesetId";

  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private YamlGitService yamlGitService;
  @InjectMocks
  private GitCommandCallback commandCallback =
      new GitCommandCallback(ACCOUNT_ID, CHANGESET_ID, GitCommandType.COMMIT_AND_PUSH);

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
}
