package software.wings.core.winrm.executors;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.utils.WinRmHelperUtil.HandleWinRmClientException;

import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultWinRmExecutor implements WinRmExecutor {
  protected DelegateLogService logService;
  private final WinRmSessionConfig config;

  DefaultWinRmExecutor(DelegateLogService logService, WinRmSessionConfig config) {
    this.logService = logService;
    this.config = config;
  }

  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    saveExecutionLog(format("Initializing WinRM connection to %s ...", config.getHostname()), LogLevel.INFO);

    try (WinRmSession session = new WinRmSession(config)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), LogLevel.INFO);
      saveExecutionLog(format("Executing command ...", config.getHostname()), LogLevel.INFO);

      StringWriter outputWriter = new StringWriter();
      StringWriter errorWriter = new StringWriter();
      int exitCode = session.executeCommandString(psWrappedCommand(command), outputWriter, errorWriter);

      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;

      String errorString = errorWriter.toString();
      String outputString = outputWriter.toString();

      if (!errorString.isEmpty()) {
        saveExecutionLog(errorString, LogLevel.ERROR);
      }

      if (!outputString.isEmpty()) {
        saveExecutionLog(outputString, LogLevel.INFO);
      }
      saveExecutionLog(format("Command completed with ExitCode (%d)", exitCode), LogLevel.INFO, commandExecutionStatus);
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      saveExecutionLog(format("Command execution failed. Error: {%s}", HandleWinRmClientException(e)), LogLevel.INFO,
          commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  private String psWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = String.format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return String.format("Powershell Invoke-Command -command {%s}", wrappedCommand);
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
            .withHostName(config.getHostname())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    return null;
  }
}
