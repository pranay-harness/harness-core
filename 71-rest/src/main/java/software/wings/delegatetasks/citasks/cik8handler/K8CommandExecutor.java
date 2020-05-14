package software.wings.delegatetasks.citasks.cik8handler;

import static java.lang.String.format;
import static java.lang.String.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.harness.exception.CommandExecutionException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.threading.Sleeper;
import io.harness.time.Timer;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class K8CommandExecutor {
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private Sleeper sleeper;
  @Inject private Timer timer;

  public static final String UTF8_ENCODING = "UTF-8";
  public static final Integer DEFAULT_DASH_COMMAND_TIMEOUT_SECS = 30; // 30 seconds
  private static final int RETRY_SLEEP_DURATION_SECS = 2;
  private static final int MAX_ERROR_RETRIES = 5;
  private static final String DASH_FILE_EXTENSION = ".sh";
  private static final String PATH_DELIMITER = "/";

  private static final String DASH_SCRIPT_CMD_FORMAT = "set -e; %s;";
  private static final String DASH_SCRIPT_WITH_RETURN_CODE_FORMAT =
      "set -e; runCmd() { RET_CODE=0; sh %s > %s 2> %s || RET_CODE=\"$?\"; echo $RET_CODE > %s; }; runCmd";
  private static final String EXEC_PROCESS_IN_BACKGROUND_FORMAT =
      "set -e; echo '%s' > %s; echo '%s' > %s; sh %s > /dev/null 2> /dev/null & echo $!";

  private static final String PROCESS_STATUS_CMD_FORMAT = "if pgrep -P %d > /dev/null; then echo %s; else echo %s; fi";
  private static final String KILL_PROCESS_CMD_FORMAT = "kill -9 %d > /dev/null";
  private static final String RETURN_STATUS_CODE_CMD_FORMAT = "cat %s";

  private static final String PROCESS_RUNNING_STATUS_CODE = "RUNNING";
  private static final String PROCESS_COMPLETE_STATUS_CODE = "COMPLETE";
  private static final int DASH_PROCESS_SUCCESS_STATUS_CODE = 0;

  public ExecCommandStatus executeCommand(KubernetesClient client, K8ExecCommandParams k8ExecCommandParams)
      throws InterruptedException, UnsupportedEncodingException {
    ShellScriptType scriptType = k8ExecCommandParams.getScriptType();
    if (scriptType != ShellScriptType.DASH) {
      String errMsg = format("Invalid script type %s to execute command", scriptType);
      throw new InvalidArgumentsException(errMsg, WingsException.USER);
    }

    return execDashCommand(client, k8ExecCommandParams);
  }

  private ExecCommandStatus execDashCommand(KubernetesClient client, K8ExecCommandParams k8ExecCommandParams)
      throws InterruptedException, UnsupportedEncodingException {
    String podName = k8ExecCommandParams.getPodName();
    String containerName = k8ExecCommandParams.getContainerName();
    String namespace = k8ExecCommandParams.getNamespace();
    Integer timeoutSecs = k8ExecCommandParams.getCommandTimeoutSecs();
    String mountPath = k8ExecCommandParams.getMountPath();
    String statusCodeFileName = getNewFileName();

    int pid = execDashCommandInBackground(client, k8ExecCommandParams, statusCodeFileName);
    boolean isComplete =
        waitForProcessCompletionWithRetries(client, podName, containerName, namespace, pid, timeoutSecs);
    if (!isComplete) {
      return ExecCommandStatus.TIMEOUT;
    }

    return getDashCmdReturnStatusCodeWithRetries(
        client, podName, containerName, namespace, mountPath, statusCodeFileName);
  }

  /**
   * Executes input specified commands as a DASH script in a background process on a K8 container and return PID for it.
   */
  private int execDashCommandInBackground(KubernetesClient client, K8ExecCommandParams k8ExecCommandParams,
      String statusCodeFileName) throws InterruptedException, UnsupportedEncodingException {
    String podName = k8ExecCommandParams.getPodName();
    String containerName = k8ExecCommandParams.getContainerName();
    String namespace = k8ExecCommandParams.getNamespace();
    List<String> commands = k8ExecCommandParams.getCommands();
    String relStdoutFilePath = k8ExecCommandParams.getRelStdoutFilePath();
    String relStderrFilePath = k8ExecCommandParams.getRelStderrFilePath();
    String mountPath = k8ExecCommandParams.getMountPath();

    String dashCmdArg =
        getExecDashCmdInBackgroundArg(commands, mountPath, relStdoutFilePath, relStderrFilePath, statusCodeFileName);
    String[] commandsToExecute = new String[] {"sh", "-c", encodeString(dashCmdArg)};
    K8ExecCommandResponse response = kubeCtlHandler.executeCommand(
        client, podName, containerName, namespace, commandsToExecute, DEFAULT_DASH_COMMAND_TIMEOUT_SECS);
    ExecCommandStatus status = response.getExecCommandStatus();
    if (status == ExecCommandStatus.SUCCESS) {
      ByteArrayOutputStream stdoutStream = response.getOutputStream();
      if (stdoutStream != null) {
        String stdout = stdoutStream.toString(UTF8_ENCODING).trim();
        try {
          return Integer.parseInt(stdout);
        } catch (NumberFormatException e) {
          logger.error("Unknown format for process return status code: {} with error: ", stdout, e);
        }
      }
    }

    throw new CommandExecutionException(
        format("Failed to execute command %s in background with status %s", commands, status));
  }

  /**
   * Returns DASH command argument to execute the input specified commands as a background process.
   */
  private String getExecDashCmdInBackgroundArg(List<String> commands, String mountPath, String relStdoutFilePath,
      String relStderrFilePath, String statusCodeFileName) {
    String stdoutFilePath = mountPath + PATH_DELIMITER + relStdoutFilePath;
    String stderrFilePath = mountPath + PATH_DELIMITER + relStderrFilePath;
    String statusCodeFilePath = mountPath + PATH_DELIMITER + statusCodeFileName;
    String cmdScriptFilePath = mountPath + PATH_DELIMITER + getNewFileName() + DASH_FILE_EXTENSION;
    String cmdWithRetCodeScriptFilePath = mountPath + PATH_DELIMITER + getNewFileName() + DASH_FILE_EXTENSION;

    // Creates a Dash script with all the commands to execute. set -e will ensure that method exit immediately if a
    // command exits with a non-zero status.
    String dashScript =
        format(DASH_SCRIPT_CMD_FORMAT, join("; ", commands), stdoutFilePath, stderrFilePath, statusCodeFilePath);

    // Creates a DASH script to execute the commands and redirect standard output, standard error and return status code
    // to files on a container.
    String dashScriptWithReturnCode = format(
        DASH_SCRIPT_WITH_RETURN_CODE_FORMAT, cmdScriptFilePath, stdoutFilePath, stderrFilePath, statusCodeFilePath);

    // This command will create a file to store the dashScript and dashScriptWithReturnCode. It will then execute the
    // script in background and output the PID on standard output.
    return format(EXEC_PROCESS_IN_BACKGROUND_FORMAT, dashScript, cmdScriptFilePath, dashScriptWithReturnCode,
        cmdWithRetCodeScriptFilePath, cmdWithRetCodeScriptFilePath);
  }

  private boolean waitForProcessCompletionWithRetries(KubernetesClient client, String podName, String containerName,
      String namespace, int pid, Integer timeoutSecs) throws InterruptedException {
    int numErrorRetries = 0;
    Instant startTime = timer.now();
    while (numErrorRetries < MAX_ERROR_RETRIES) {
      try {
        return waitForProcessCompletion(client, podName, containerName, namespace, pid, timeoutSecs, startTime);
      } catch (CommandExecutionException | KubernetesClientException | UnsupportedEncodingException e) {
        logger.warn("Failed to check status of process with error: ", e);
        numErrorRetries += 1;

        if (numErrorRetries < MAX_ERROR_RETRIES) {
          sleeper.sleep(TimeUnit.SECONDS.toMillis(RETRY_SLEEP_DURATION_SECS));
        }
      }
    }

    throw new CommandExecutionException(
        format("Failed to check running status of background process %d with retries", pid));
  }

  // Waits for process to complete within a timeout. If complete, it return success. Otherwise, it kill the process and
  // return failure.
  private boolean waitForProcessCompletion(KubernetesClient client, String podName, String containerName,
      String namespace, int pid, Integer timeoutSecs, Instant startTime)
      throws InterruptedException, UnsupportedEncodingException {
    Duration elapsedTime = Duration.ZERO;
    while (elapsedTime.getSeconds() < timeoutSecs) {
      boolean isRunning = isProcessRunning(client, podName, containerName, namespace, pid);
      if (!isRunning) {
        return true;
      }

      sleeper.sleep(TimeUnit.SECONDS.toMillis(RETRY_SLEEP_DURATION_SECS));
      Instant currTime = timer.now();
      elapsedTime = Duration.between(startTime, currTime);
    }

    // If process is still in running status, kill the background process.
    killProcess(client, podName, containerName, namespace, pid);
    return false;
  }

  private boolean isProcessRunning(KubernetesClient client, String podName, String containerName, String namespace,
      int pid) throws InterruptedException, UnsupportedEncodingException {
    String dashCmd = format(PROCESS_STATUS_CMD_FORMAT, pid, PROCESS_RUNNING_STATUS_CODE, PROCESS_COMPLETE_STATUS_CODE);
    String[] commandsToExecute = new String[] {"sh", "-c", encodeString(dashCmd)};

    K8ExecCommandResponse response = kubeCtlHandler.executeCommand(
        client, podName, containerName, namespace, commandsToExecute, DEFAULT_DASH_COMMAND_TIMEOUT_SECS);
    ExecCommandStatus status = response.getExecCommandStatus();
    if (status == ExecCommandStatus.SUCCESS) {
      ByteArrayOutputStream stdoutStream = response.getOutputStream();
      if (stdoutStream != null) {
        String stdout = stdoutStream.toString(UTF8_ENCODING).trim();
        if (stdout.equals(PROCESS_RUNNING_STATUS_CODE)) {
          return true;
        } else if (stdout.equals(PROCESS_COMPLETE_STATUS_CODE)) {
          return false;
        }
      }
    }

    throw new CommandExecutionException(
        format("Failed to check status of background process using command %s with status %s", dashCmd, status));
  }

  // Kills process with a process ID in a best effort case
  private void killProcess(KubernetesClient client, String podName, String containerName, String namespace, int pid)
      throws InterruptedException {
    String dashCmd = format(KILL_PROCESS_CMD_FORMAT, pid);
    String[] commandsToExecute = new String[] {"sh", "-c", encodeString(dashCmd)};

    kubeCtlHandler.executeCommand(
        client, podName, containerName, namespace, commandsToExecute, DEFAULT_DASH_COMMAND_TIMEOUT_SECS);
  }

  private ExecCommandStatus getDashCmdReturnStatusCodeWithRetries(KubernetesClient client, String podName,
      String containerName, String namespace, String mountPath, String statusCodeFileName) throws InterruptedException {
    int numRetries = 0;
    while (numRetries < MAX_ERROR_RETRIES) {
      try {
        return getDashCmdReturnStatusCode(client, podName, containerName, namespace, mountPath, statusCodeFileName);
      } catch (CommandExecutionException | UnsupportedEncodingException e) {
        logger.warn("Failed to check return status code of process with error: ", e);
        numRetries += 1;
        if (numRetries < MAX_ERROR_RETRIES) {
          sleeper.sleep(TimeUnit.SECONDS.toMillis(RETRY_SLEEP_DURATION_SECS));
        }
      }
    }

    throw new CommandExecutionException(
        format("Failed to check return status code of process from file %s with retries", statusCodeFileName));
  }

  private ExecCommandStatus getDashCmdReturnStatusCode(KubernetesClient client, String podName, String containerName,
      String namespace, String mountPath, String statusCodeFileName)
      throws InterruptedException, UnsupportedEncodingException {
    String statusCodeFilePath = mountPath + PATH_DELIMITER + statusCodeFileName;
    String dashCmd = format(RETURN_STATUS_CODE_CMD_FORMAT, statusCodeFilePath);
    String[] commandsToExecute = new String[] {"sh", "-c", encodeString(dashCmd)};
    K8ExecCommandResponse response = kubeCtlHandler.executeCommand(
        client, podName, containerName, namespace, commandsToExecute, DEFAULT_DASH_COMMAND_TIMEOUT_SECS);
    if (response.getExecCommandStatus() == ExecCommandStatus.TIMEOUT
        || response.getExecCommandStatus() == ExecCommandStatus.ERROR) {
      throw new CommandExecutionException(
          format("Failed to check return status of process with error %s", response.getExecCommandStatus()));
    }

    ExecCommandStatus cmdExecutionStatus = ExecCommandStatus.ERROR;
    if (response.getExecCommandStatus() == ExecCommandStatus.SUCCESS) {
      ByteArrayOutputStream stdoutStream = response.getOutputStream();
      if (stdoutStream != null) {
        String stdout = stdoutStream.toString(UTF8_ENCODING).trim();
        try {
          int returnStatusCode = Integer.parseInt(stdout);
          if (returnStatusCode == DASH_PROCESS_SUCCESS_STATUS_CODE) {
            cmdExecutionStatus = ExecCommandStatus.SUCCESS;
          } else {
            cmdExecutionStatus = ExecCommandStatus.FAILURE;
          }
        } catch (NumberFormatException e) {
          logger.error("Unknown format for process return status code: {} with error: ", stdout, e);
        }
      }
    }
    return cmdExecutionStatus;
  }

  private String getNewFileName() {
    return UUID.randomUUID().toString();
  }

  private String encodeString(String s) {
    try {
      return URLEncoder.encode(s, UTF8_ENCODING);
    } catch (UnsupportedEncodingException e) {
      String errMsg = format("Failed to encode string %s with %s format", s, UTF8_ENCODING);
      throw new InvalidArgumentsException(errMsg, WingsException.USER, e);
    }
  }
}