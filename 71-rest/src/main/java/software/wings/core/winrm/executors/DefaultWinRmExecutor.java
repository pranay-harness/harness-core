package software.wings.core.winrm.executors;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.utils.WinRmHelperUtil.GetErrorDetailsFromWinRmClientException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.ResponseMessage;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.ExecutionLogWriter;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultWinRmExecutor implements WinRmExecutor {
  private static final Logger logger = LoggerFactory.getLogger(DefaultWinRmExecutor.class);

  protected DelegateLogService logService;
  private final WinRmSessionConfig config;

  DefaultWinRmExecutor(DelegateLogService logService, WinRmSessionConfig config) {
    this.logService = logService;
    this.config = config;
  }

  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null);
  }

  public CommandExecutionStatus executeCommandString(String command, boolean displayCommand) {
    return executeCommandString(command, null, displayCommand);
  }

  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    return executeCommandString(command, output, false);
  }

  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    saveExecutionLog(format("Initializing WinRM connection to %s ...", config.getHostname()), INFO);

    try (WinRmSession session = new WinRmSession(config)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      if (displayCommand) {
        saveExecutionLog(format("Executing command %s...", command), INFO);
      } else {
        saveExecutionLog(format("Executing command ..."), INFO);
      }

      ExecutionLogWriter outputWriter = ExecutionLogWriter.builder()
                                            .accountId(config.getAccountId())
                                            .appId(config.getAppId())
                                            .commandUnitName(config.getCommandUnitName())
                                            .executionId(config.getExecutionId())
                                            .hostName(config.getHostname())
                                            .logService(logService)
                                            .stringBuilder(new StringBuilder(1024))
                                            .logLevel(INFO)
                                            .build();

      ExecutionLogWriter errorWriter = ExecutionLogWriter.builder()
                                           .accountId(config.getAccountId())
                                           .appId(config.getAppId())
                                           .commandUnitName(config.getCommandUnitName())
                                           .executionId(config.getExecutionId())
                                           .hostName(config.getHostname())
                                           .logService(logService)
                                           .stringBuilder(new StringBuilder(1024))
                                           .logLevel(ERROR)
                                           .build();

      int exitCode = session.executeCommandString(psWrappedCommand(command), outputWriter, errorWriter);
      commandExecutionStatus = (exitCode == 0 && !errorWriter.didWriteGetCalled()) ? SUCCESS : FAILURE;
      saveExecutionLog(format("Command completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      logger.error("Error while executing command", e);
      ResponseMessage details = GetErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  private String psWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return format("Powershell Invoke-Command -command {%s}", wrappedCommand);
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

  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    throw new NotImplementedException("Not implemented");
  }

  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath,
      ArtifactStreamAttributes artifactStreamAttributes, String accountId, String appId, String activityId,
      String commandUnitName, String hostName) {
    throw new NotImplementedException("Not implemented");
  }

  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    throw new NotImplementedException("Not implemented");
  }

  public CommandExecutionStatus copyGridFsFiles(ConfigFileMetaData configFileMetaData) {
    throw new NotImplementedException("Not implemented");
  }
}