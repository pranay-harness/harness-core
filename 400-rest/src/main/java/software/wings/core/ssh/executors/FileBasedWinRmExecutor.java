package software.wings.core.ssh.executors;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.utils.WinRmHelperUtils.buildErrorDetailsFromWinRmClientException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.service.DelegateAgentFileService;
import io.harness.eraro.ResponseMessage;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.utils.ExecutionLogWriter;

import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class FileBasedWinRmExecutor implements FileBasedScriptExecutor {
  public static final String HARNESS_FILENAME_PREFIX = "\\harness-";
  public static final String WINDOWS_TEMPFILE_LOCATION = "%TEMP%";
  public static final String NOT_IMPLEMENTED = "Not implemented";
  private static final String ERROR_WHILE_EXECUTING_COMMAND = "Error while executing command";

  protected LogCallback logCallback;
  private final WinRmSessionConfig config;
  protected DelegateFileManager delegateFileManager;
  private boolean disableCommandEncoding;
  private boolean shouldSaveExecutionLogs;
  private final String powershell;

  public FileBasedWinRmExecutor(LogCallback logCallback, DelegateFileManager delegateFileManager,
      boolean shouldSaveExecutionLogs, WinRmSessionConfig config, boolean disableCommandEncoding) {
    this.logCallback = logCallback;
    this.delegateFileManager = delegateFileManager;
    this.shouldSaveExecutionLogs = shouldSaveExecutionLogs;
    this.config = config;
    this.disableCommandEncoding = disableCommandEncoding;

    if (this.config.isUseNoProfile()) {
      powershell = "Powershell -NoProfile ";
    } else {
      powershell = "Powershell ";
    }
  }

  @Override
  public CommandExecutionStatus copyConfigFiles(CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData) {
    CommandExecutionStatus commandExecutionStatus;
    if (isBlank(configFileMetaData.getFileId()) || isBlank(configFileMetaData.getFilename())) {
      saveExecutionLog("There is no config file to copy. " + configFileMetaData.toString(), INFO);
      return CommandExecutionStatus.SUCCESS;
    }
    commandExecutionStatus = downloadConfigFile(configFileMetaData);
    log.info("Copy Config command execution returned.", commandExecutionStatus);
    return commandExecutionStatus;
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    throw new NotImplementedException(NOT_IMPLEMENTED);
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath,
      ArtifactStreamAttributes artifactStreamAttributes, String accountId, String appId, String activityId,
      String commandUnitName, String hostName) {
    throw new NotImplementedException(NOT_IMPLEMENTED);
  }

  @Override
  public CommandExecutionStatus copyGridFsFiles(String destinationDirectoryPath,
      DelegateAgentFileService.FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    throw new NotImplementedException(NOT_IMPLEMENTED);
  }

  private CommandExecutionStatus downloadConfigFile(CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try (WinRmSession session = new WinRmSession(config, this.logCallback);
         ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      saveExecutionLog(format("Executing command ...%n"), INFO);

      byte[] fileBytes = new byte[configFileMetaData.getLength().intValue()];

      try (InputStream inputStream = delegateFileManager.downloadByConfigFileId(configFileMetaData.getFileId(),
               config.getAccountId(), config.getAppId(), configFileMetaData.getActivityId())) {
        // Copy config file content
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        int configSize = configFileMetaData.getLength().intValue();
        byte[] data = new byte[configSize];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }

        fileBytes = buffer.toByteArray();
      } catch (Exception e) {
        log.error("Error while downloading config file.", e);
      }

      int exitCode;
      String command;
      String psScriptFile = null;

      if (disableCommandEncoding) {
        // Keep the temp script in working directory or in Temp is working directory is not set.
        psScriptFile = config.getWorkingDirectory() == null
            ? WINDOWS_TEMPFILE_LOCATION
            : config.getWorkingDirectory() + "harness-" + this.config.getExecutionId() + ".ps1";
        command = getCopyConfigCommandBehindFF(configFileMetaData, fileBytes);
        exitCode = session.executeCommandsList(
            WinRmExecutorHelper.constructPSScriptWithCommands(command, psScriptFile, powershell), outputWriter,
            errorWriter, false);
      } else {
        String encodedFile = EncodingUtils.encodeBase64(fileBytes);
        command = getCopyConfigCommand(configFileMetaData, encodedFile);

        exitCode = session.executeCommandString(
            WinRmExecutorHelper.psWrappedCommandWithEncoding(command, powershell), outputWriter, errorWriter, false);
      }
      log.info("Execute Command String returned exit code.", exitCode);
      commandExecutionStatus = SUCCESS;
      WinRmExecutorHelper.cleanupFiles(session, psScriptFile, powershell, disableCommandEncoding);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      log.error(ERROR_WHILE_EXECUTING_COMMAND, e);
      ResponseMessage details = buildErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  private void saveExecutionLog(String line, LogLevel level) {
    saveExecutionLog(line, level, RUNNING);
  }

  private void saveExecutionLog(String line, LogLevel level, CommandExecutionStatus commandExecutionStatus) {
    if (shouldSaveExecutionLogs) {
      logCallback.saveExecutionLog(line, level, commandExecutionStatus);
    }
  }

  private ExecutionLogWriter getExecutionLogWriter(LogLevel logLevel) {
    return ExecutionLogWriter.builder()
        .accountId(config.getAccountId())
        .appId(config.getAppId())
        .commandUnitName(config.getCommandUnitName())
        .executionId(config.getExecutionId())
        .logCallback(logCallback)
        .stringBuilder(new StringBuilder(1024))
        .logLevel(logLevel)
        .build();
  }

  @VisibleForTesting
  public String getCopyConfigCommandBehindFF(
      CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData, byte[] fileBytes) {
    return "$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\" + configFileMetaData.getFilename()
        + "\"\n"
        + "$commandString = {" + new String(fileBytes) + "}"
        + "\n[IO.File]::WriteAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
        + "Write-Host \"Copied config file to the host.\"\n";
  }

  @VisibleForTesting
  public String getCopyConfigCommand(CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData, String encodedFile) {
    return "#### Convert Base64 string back to config file ####\n"
        + "\n"
        + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\"" + encodedFile
        + "\"))\n"
        + "Write-Host \"Decoding config file on the host.\"\n"
        + "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "[IO.File]::WriteAllText($decodedFile, $DecodedString) \n"
        + "Write-Host \"Copied config file to the host.\"\n";
  }
}
