package software.wings.delegatetasks.citasks.cik8handler;

import static java.lang.String.format;
import static java.lang.String.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Singleton
public class K8CommandExecutor {
  @Inject private CIK8CtlHandler kubeCtlHandler;

  public static final Integer DEFAULT_COMMAND_EXEC_TIMEOUT_SECS = 60 * 60; // 1 hour
  public static final String COMMAND_EXEC_URL_ENCODE_FORMAT = "UTF-8";
  private static final String EXEC_CMD_FORMAT = "runCmd() { set -e; %s; }; runCmd > %s 2> %s";

  public boolean executeCommand(KubernetesClient kubernetesClient, K8ExecCommandParams k8ExecCommandParams)
      throws InterruptedException, TimeoutException {
    String podName = k8ExecCommandParams.getPodName();
    String containerName = k8ExecCommandParams.getContainerName();
    String namespace = k8ExecCommandParams.getNamespace();
    List<String> commands = k8ExecCommandParams.getCommands();
    Integer timeoutSecs = k8ExecCommandParams.getCommandTimeoutSecs();
    String stdoutFilePath = k8ExecCommandParams.getStdoutFilePath();
    String stderrFilePath = k8ExecCommandParams.getStderrFilePath();
    ShellScriptType scriptType = k8ExecCommandParams.getScriptType();

    if (scriptType != ShellScriptType.DASH) {
      String errMsg = format("Invalid script type %s to execute command", scriptType);
      throw new InvalidArgumentsException(errMsg, WingsException.USER);
    }

    String dashCommand = execDashCommandArg(commands, stdoutFilePath, stderrFilePath);
    String[] commandsToExecute = new String[] {"sh", "-c", dashCommand};
    if (timeoutSecs == null || timeoutSecs == 0) {
      timeoutSecs = DEFAULT_COMMAND_EXEC_TIMEOUT_SECS;
    }

    return kubeCtlHandler.executeCommand(
        kubernetesClient, podName, containerName, namespace, commandsToExecute, timeoutSecs);
  }

  /**
   * Returns DASH shell command to execute the input specified commands.
   */
  private String execDashCommandArg(List<String> commands, String stdoutFilePath, String stderrFilePath) {
    // Creates a Dash method with all the commands specified and outputs the standard output and error to a file on
    // specified paths. set -e will ensure that method exit immediately if a command exits with a non-zero status.
    String cmd = format(EXEC_CMD_FORMAT, join("; ", commands), stdoutFilePath, stderrFilePath);
    String encodedCmd = null;
    try {
      encodedCmd = URLEncoder.encode(cmd, COMMAND_EXEC_URL_ENCODE_FORMAT);
    } catch (UnsupportedEncodingException e) {
      String errMsg = format("Failed to encode commands %s with %s format", commands, COMMAND_EXEC_URL_ENCODE_FORMAT);
      throw new InvalidArgumentsException(errMsg, WingsException.USER, e);
    }

    return encodedCmd;
  }
}
