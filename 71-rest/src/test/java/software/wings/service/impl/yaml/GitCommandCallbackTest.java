package software.wings.service.impl.yaml;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.eraro.ErrorCode;
import io.harness.task.protocol.ResponseData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;

import java.util.HashMap;
import java.util.Map;

public class GitCommandCallbackTest {
  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private YamlGitService yamlGitService;
  @InjectMocks private GitCommandCallback commandCallback = new GitCommandCallback("111", "222", "333");

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
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
      assertTrue(false);
    } catch (RuntimeException e) {
      // the way we are testing here is, for GitCommandStatus.SUCCESS, closeAlertForGitFailureIfOpen() should get
      // called. in mock, we are throwing exception on this method call as this is what we want to test. rest of code
      // flow is not required for this unit test
      assertTrue(true);
    }
  }
}
