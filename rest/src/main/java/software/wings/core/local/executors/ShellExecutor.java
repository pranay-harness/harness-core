package software.wings.core.local.executors;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.ScriptType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class ShellExecutor {
  private DelegateLogService logService;
  private ShellExecutorConfig config;
  private ScriptType scriptType;

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

    String[] commandList = null;

    switch (this.scriptType) {
      case BASH:
        commandList = new String[] {"/bin/bash", "-c", bashWrappedCommand(command)};
        break;

      case POWERSHELL:
        commandList = new String[] {"Powershell", "-c", psWrappedCommand(command)};
        break;

      default:
        unhandled(this.scriptType);
    }

    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(new File(config.getWorkingDirectory()))
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
      commandExecutionStatus = FAILURE;
      saveExecutionLog(format("Exception: %s", e.getCause()), ERROR, commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  private String psWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = String.format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return String.format("Invoke-Command -command {%s}", wrappedCommand);
  }

  private String bashWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    return String.format("\"eval $(echo %s|base64 -d)\"", base64Command);
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