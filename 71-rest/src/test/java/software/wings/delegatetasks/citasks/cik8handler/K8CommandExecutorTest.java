package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.category.element.UnitTests;
import io.harness.exception.CommandExecutionException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.threading.Sleeper;
import io.harness.time.Timer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class K8CommandExecutorTest extends WingsBaseTest {
  @Mock private CIK8CtlHandler cik8CtlHandler;
  @Mock private Sleeper sleeper;
  @Mock private Timer timer;
  @InjectMocks private K8CommandExecutor k8CommandExecutor;

  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String namespace = "default";
  private static final List<String> commands = Arrays.asList("ls", "cd dir", "ls");

  private static final String mountPath = "/step";
  private static final String stdoutFilePath = "dir/stdout";
  private static final String stderrFilePath = "dir/stderr";
  private static final String dashShellStr = "sh";
  private static final String dashShellArg = "-c";

  private static final Integer pid = 7;
  private static final String emptyOutputStream = "";
  private static final Integer timeoutSecs = 300;

  private static final String PROCESS_RUNNING_STATUS_CODE = "RUNNING";
  private static final String PROCESS_COMPLETE_STATUS_CODE = "COMPLETE";
  private static final Integer DASH_PROCESS_SUCCESS_STATUS_CODE = 0;
  private static final Integer DASH_PROCESS_NON_ZERO_STATUS_CODE = 127;

  private K8ExecCommandParams getParams(ShellScriptType scriptType, Integer timeoutSecs) {
    return K8ExecCommandParams.builder()
        .podName(podName)
        .commands(commands)
        .containerName(containerName)
        .mountPath(mountPath)
        .relStdoutFilePath(stdoutFilePath)
        .relStderrFilePath(stderrFilePath)
        .namespace(namespace)
        .commandTimeoutSecs(timeoutSecs)
        .scriptType(scriptType)
        .build();
  }

  private K8ExecCommandResponse getResponse(ExecCommandStatus status, String data) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(outputStream);

    try {
      out.write(data.getBytes());
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      logger.info("Output stream error: ", e);
    }
    return K8ExecCommandResponse.builder().execCommandStatus(status).outputStream(outputStream).build();
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithInvalidArgument() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.POWERSHELL, timeoutSecs);

    k8CommandExecutor.executeCommand(client, params);
  }

  @Test(expected = CommandExecutionException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithFailureInBackgroundProcessExec()
      throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.FAILURE, emptyOutputStream);

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenReturn(backgroundProcessExecResponse);

    k8CommandExecutor.executeCommand(client, params);
  }

  @Test(expected = CommandExecutionException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithFailureInBackgroundProcessExecNoPid()
      throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, emptyOutputStream);

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenReturn(backgroundProcessExecResponse);

    k8CommandExecutor.executeCommand(client, params);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithSuccess() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);
    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_SUCCESS_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processCompletedStatus;
            } else {
              return processReturnCodeStatus;
            }
          }
        });
    when(timer.now()).thenReturn(Instant.now());

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.SUCCESS, execCommandStatus);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithProcessStatusRetry() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processRunningStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_RUNNING_STATUS_CODE);
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);

    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_SUCCESS_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processRunningStatus;
            } else if (count == 3) {
              return processCompletedStatus;
            } else {
              return processReturnCodeStatus;
            }
          }
        });
    doNothing().when(sleeper).sleep(anyLong());

    when(timer.now()).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocationOnMock) {
        return Instant.now();
      }
    });

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.SUCCESS, execCommandStatus);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithProcessStatusKillProcess() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);
    int startTimeSeconds = 0;

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processRunningStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_RUNNING_STATUS_CODE);
    K8ExecCommandResponse killProcessStatus = getResponse(ExecCommandStatus.SUCCESS, emptyOutputStream);
    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_SUCCESS_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processRunningStatus;
            } else {
              return killProcessStatus;
            }
          }
        });
    doNothing().when(sleeper).sleep(anyLong());

    when(timer.now()).thenAnswer(new Answer() {
      private int count;

      public Object answer(InvocationOnMock invocationOnMock) {
        count = count + 1;
        if (count == 1) {
          return Instant.ofEpochSecond(startTimeSeconds);
        } else {
          return Instant.ofEpochSecond(startTimeSeconds + timeoutSecs + 1);
        }
      }
    });

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.TIMEOUT, execCommandStatus);
  }

  @Test(expected = CommandExecutionException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithProcessStatusExhaustedErrorRetries()
      throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processRunningErrorStatus = getResponse(ExecCommandStatus.ERROR, emptyOutputStream);
    K8ExecCommandResponse killProcessStatus = getResponse(ExecCommandStatus.SUCCESS, emptyOutputStream);
    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_SUCCESS_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else {
              return processRunningErrorStatus;
            }
          }
        });
    doNothing().when(sleeper).sleep(anyLong());
    when(timer.now()).thenReturn(Instant.now());

    k8CommandExecutor.executeCommand(client, params);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithEmptyReturnStatusCodeFile() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);
    K8ExecCommandResponse processReturnCodeStatus = getResponse(ExecCommandStatus.SUCCESS, emptyOutputStream);

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processCompletedStatus;
            } else {
              return processReturnCodeStatus;
            }
          }
        });
    when(timer.now()).thenReturn(Instant.now());

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.ERROR, execCommandStatus);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithNonZeroReturnStatusCode() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);
    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_NON_ZERO_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processCompletedStatus;
            } else {
              return processReturnCodeStatus;
            }
          }
        });
    when(timer.now()).thenReturn(Instant.now());

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.FAILURE, execCommandStatus);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithReturnStatusCodeRetries() throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);
    K8ExecCommandResponse processReturnCodeTimeoutStatus = getResponse(ExecCommandStatus.TIMEOUT, emptyOutputStream);

    K8ExecCommandResponse processReturnCodeStatus =
        getResponse(ExecCommandStatus.SUCCESS, DASH_PROCESS_SUCCESS_STATUS_CODE.toString());

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processCompletedStatus;
            } else if (count == 3) {
              return processReturnCodeTimeoutStatus;
            } else {
              return processReturnCodeStatus;
            }
          }
        });
    when(timer.now()).thenReturn(Instant.now());
    doNothing().when(sleeper).sleep(anyLong());

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
    assertEquals(ExecCommandStatus.SUCCESS, execCommandStatus);
  }

  @Test(expected = CommandExecutionException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithReturnStatusCodeExhaustedRetries()
      throws InterruptedException, UnsupportedEncodingException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);

    K8ExecCommandResponse backgroundProcessExecResponse = getResponse(ExecCommandStatus.SUCCESS, pid.toString());
    K8ExecCommandResponse processCompletedStatus = getResponse(ExecCommandStatus.SUCCESS, PROCESS_COMPLETE_STATUS_CODE);
    K8ExecCommandResponse processReturnCodeErrorStatus = getResponse(ExecCommandStatus.ERROR, emptyOutputStream);

    when(cik8CtlHandler.executeCommand(eq(client), eq(podName), eq(containerName), eq(namespace), any(), any()))
        .thenAnswer(new Answer() {
          private int count;

          public Object answer(InvocationOnMock invocation) {
            count = count + 1;
            if (count == 1) {
              return backgroundProcessExecResponse;
            } else if (count == 2) {
              return processCompletedStatus;
            } else {
              return processReturnCodeErrorStatus;
            }
          }
        });
    when(timer.now()).thenReturn(Instant.now());
    doNothing().when(sleeper).sleep(anyLong());

    ExecCommandStatus execCommandStatus = k8CommandExecutor.executeCommand(client, params);
  }
}