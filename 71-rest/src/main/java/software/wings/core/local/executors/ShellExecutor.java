package software.wings.core.local.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.ScriptType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ShellExecutor {
  private DelegateLogService logService;
  private ShellExecutorConfig config;
  private ScriptType scriptType;

  private static final String defaultParentWorkingDirectory = "./local-scripts/";
  static {
    try {
      createDirectoryIfDoesNotExist(defaultParentWorkingDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create local-scripts directory", e);
    }
  }

  ShellExecutor(DelegateLogService logService, ShellExecutorConfig config, ScriptType scriptType) {
    this.logService = logService;
    this.config = config;
    this.scriptType = scriptType;
  }

  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null);
  }

  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    saveExecutionLog(format("Executing command ..."), INFO);

    switch (this.scriptType) {
      case BASH:
        try {
          commandExecutionStatus = executeBashScript(command);
        } catch (Exception e) {
          saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
        }
        break;

      case POWERSHELL:
        commandExecutionStatus = executePowerShellScript(command);
        break;

      default:
        unhandled(this.scriptType);
    }

    return commandExecutionStatus;
  }

  private CommandExecutionStatus executeBashScript(String command) throws IOException {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    File workingDirectory;

    if (isEmpty(config.getWorkingDirectory())) {
      String directoryPath = defaultParentWorkingDirectory + config.getExecutionId();
      createDirectoryIfDoesNotExist(directoryPath);
      workingDirectory = new File(directoryPath);
    } else {
      workingDirectory = new File(config.getWorkingDirectory());
    }

    String scriptFilename = "harness-" + this.config.getExecutionId() + ".sh";
    String kubeConfigFilename = "kube-" + this.config.getExecutionId() + "-config";
    File scriptFile = new File(workingDirectory, scriptFilename);
    File kubeConfigFile = new File(workingDirectory, kubeConfigFilename);

    Map<String, String> environment = config.getEnvironment();

    if (!isEmpty(config.getKubeConfigContent())) {
      try (FileOutputStream outputStream = new FileOutputStream(kubeConfigFile)) {
        outputStream.write(config.getKubeConfigContent().getBytes("UTF-8"));
        environment.put(HARNESS_KUBE_CONFIG_PATH, kubeConfigFile.getCanonicalPath());
      }
    }

    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
      outputStream.write(command.getBytes("UTF-8"));

      String[] commandList = new String[] {"/bin/bash", scriptFilename};
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(workingDirectory)
                                            .environment(environment)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), INFO);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), ERROR);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      saveExecutionLog(
          format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO, commandExecutionStatus);
    } catch (Exception e) {
      saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
    } finally {
      if (isEmpty(config.getWorkingDirectory())) {
        deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      } else {
        deleteFileIfExists(scriptFile.getAbsolutePath());
        deleteFileIfExists(kubeConfigFile.getAbsolutePath());
      }
    }

    return commandExecutionStatus;
  }

  private CommandExecutionStatus executePowerShellScript(String command) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    File workingDirectory = isEmpty(config.getWorkingDirectory()) ? new File(defaultParentWorkingDirectory)
                                                                  : new File(config.getWorkingDirectory());

    String[] commandList = new String[] {"Powershell", "-c", psWrappedCommand(command)};
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(workingDirectory)
                                            .environment(config.getEnvironment())
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), INFO);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), ERROR);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      saveExecutionLog(
          format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO, commandExecutionStatus);
    } catch (Exception e) {
      saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
    }

    return commandExecutionStatus;
  }

  private String psWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return format("Invoke-Command -command {%s}", wrappedCommand);
  }

  private void saveExecutionLog(String line, LogLevel level) {
    saveExecutionLog(line, level, RUNNING);
  }

  private void saveExecutionLog(String line, LogLevel level, CommandExecutionStatus commandExecutionStatus) {
    logService.save(config.getAccountId(),
        aLog()
            .withAppId(config.getAppId())
            .withActivityId(config.getExecutionId())
            .withLogLevel(level)
            .withCommandUnitName(config.getCommandUnitName())
            .withHostName("localhost")
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }
}
