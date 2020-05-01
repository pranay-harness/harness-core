package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class K8CommandExecutorTest extends WingsBaseTest {
  @Mock private CIK8CtlHandler cik8CtlHandler;
  @InjectMocks private K8CommandExecutor k8CommandExecutor;

  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String namespace = "default";
  private static final List<String> commands = Arrays.asList("ls", "cd dir", "ls");

  private static final String stdoutFilePath = "dir/stdout";
  private static final String stderrFilePath = "dir/stderr";
  private static final String encodedDashCmd =
      "runCmd%28%29+%7B+set+-e%3B+ls%3B+cd+dir%3B+ls%3B+%7D%3B+runCmd+%3E+dir%2Fstdout+2%3E+dir%2Fstderr";
  private static final String dashShellStr = "sh";
  private static final String dashShellArg = "-c";
  private static final String[] encodedCmdList = new String[] {dashShellStr, dashShellArg, encodedDashCmd};
  private static final Integer defaultTimeoutSecs = 60 * 60;
  private static final Integer timeoutSecs = 10;

  private K8ExecCommandParams getParams(ShellScriptType scriptType, Integer timeoutSecs) {
    return K8ExecCommandParams.builder()
        .podName(podName)
        .commands(commands)
        .containerName(containerName)
        .stdoutFilePath(stdoutFilePath)
        .stderrFilePath(stderrFilePath)
        .namespace(namespace)
        .commandTimeoutSecs(timeoutSecs)
        .scriptType(scriptType)
        .build();
  }
  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithSuccess() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, null);
    when(cik8CtlHandler.executeCommand(client, podName, containerName, namespace, encodedCmdList, defaultTimeoutSecs))
        .thenReturn(Boolean.TRUE);
    assertTrue(k8CommandExecutor.executeCommand(client, params));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithFailure() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.DASH, timeoutSecs);
    when(cik8CtlHandler.executeCommand(client, podName, containerName, namespace, encodedCmdList, timeoutSecs))
        .thenReturn(Boolean.FALSE);
    assertFalse(k8CommandExecutor.executeCommand(client, params));
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithException() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    K8ExecCommandParams params = getParams(ShellScriptType.POWERSHELL, null);
    when(cik8CtlHandler.executeCommand(client, podName, containerName, namespace, encodedCmdList, defaultTimeoutSecs))
        .thenReturn(Boolean.TRUE);
    k8CommandExecutor.executeCommand(client, params);
  }
}