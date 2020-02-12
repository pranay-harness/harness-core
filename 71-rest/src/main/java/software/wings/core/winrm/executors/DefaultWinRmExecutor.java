package software.wings.core.winrm.executors;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.utils.WinRmHelperUtils.buildErrorDetailsFromWinRmClientException;

import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionResultBuilder;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.ExecutionLogWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DefaultWinRmExecutor implements WinRmExecutor {
  protected DelegateLogService logService;
  private final WinRmSessionConfig config;
  protected DelegateFileManager delegateFileManager;

  DefaultWinRmExecutor(
      DelegateLogService logService, DelegateFileManager delegateFileManager, WinRmSessionConfig config) {
    this.logService = logService;
    this.delegateFileManager = delegateFileManager;
    this.config = config;
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null, false);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, boolean displayCommand) {
    return executeCommandString(command, null, displayCommand);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    return executeCommandString(command, output, false);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    CommandExecutionStatus commandExecutionStatus;
    saveExecutionLog(format("Initializing WinRM connection to %s ...", config.getHostname()), INFO);

    try (WinRmSession session = new WinRmSession(config); ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      if (displayCommand) {
        saveExecutionLog(format("Executing command %s...", command), INFO);
      } else {
        saveExecutionLog(format("Executing command ...%n"), INFO);
      }

      int exitCode = session.executeCommandString(psWrappedCommand(command), outputWriter, errorWriter);
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;
      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      logger.error("Error while executing command", e);
      ResponseMessage details = buildErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  private String addEnvVariablesCollector(
      String command, List<String> envVariablesToCollect, String envVariablesOutputFileName) {
    StringBuilder wrapperCommand = new StringBuilder(command);
    wrapperCommand.append('\n');
    for (String env : envVariablesToCollect) {
      wrapperCommand.append("$e = \"")
          .append(env)
          .append("=\"\n $e+=$Env:")
          .append(env)
          .append("\n Write-Output $e | Out-File -Encoding UTF8 -append -FilePath ")
          .append(envVariablesOutputFileName)
          .append('\n');
    }
    return wrapperCommand.toString();
  }

  @Override
  public CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect) {
    ShellExecutionDataBuilder executionDataBuilder = ShellExecutionData.builder();
    CommandExecutionResultBuilder commandExecutionResult = CommandExecutionResult.builder();
    CommandExecutionStatus commandExecutionStatus;
    saveExecutionLog(format("Initializing WinRM connection to %s ...", config.getHostname()), INFO);
    String envVariablesOutputFile = null;

    if (!envVariablesToCollect.isEmpty()) {
      envVariablesOutputFile = this.config.getWorkingDirectory() + "\\"
          + "harness-" + this.config.getExecutionId() + ".out";
    }
    try (WinRmSession session = new WinRmSession(config); ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      saveExecutionLog(format("Executing command ...%n"), INFO);

      command = addEnvVariablesCollector(command, envVariablesToCollect, envVariablesOutputFile);
      int exitCode = session.executeCommandString(psWrappedCommand(command), outputWriter, errorWriter);
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;

      if (commandExecutionStatus == SUCCESS && envVariablesOutputFile != null) {
        executionDataBuilder.sweepingOutputEnvVariables(
            collectOutputEnvironmentVariables(session, envVariablesOutputFile));
      }

      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
      commandExecutionResult.status(commandExecutionStatus);
      commandExecutionResult.commandExecutionData(executionDataBuilder.build());
      return commandExecutionResult.build();
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      logger.error("Error while executing command", e);
      ResponseMessage details = buildErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }
    commandExecutionResult.status(commandExecutionStatus);
    commandExecutionResult.commandExecutionData(executionDataBuilder.build());
    return commandExecutionResult.build();
  }

  private Map<String, String> collectOutputEnvironmentVariables(WinRmSession session, String envVariablesOutputFile) {
    Map<String, String> envVariablesMap = new HashMap<>();
    String command = "$base64string = [Convert]::ToBase64String([IO.File]::ReadAllBytes('" + envVariablesOutputFile
        + "'))\n"
        + "Write-Host $base64string -NoNewline";

    try (StringWriter outputAccumulator = new StringWriter(1024);
         StringWriter errorAccumulator = new StringWriter(1024)) {
      int exitCode = session.executeCommandString(psWrappedCommand(command), outputAccumulator, errorAccumulator);
      if (exitCode != 0) {
        logger.error("Powershell script collecting output environment Variables failed with exitCode {}", exitCode);
        return envVariablesMap;
      }

      byte[] decodedBytes = Base64.getDecoder().decode(outputAccumulator.getBuffer().toString());
      // removing UTF-8 and UTF-16 BOMs if any
      String fileContents =
          new String(decodedBytes, Charset.forName("UTF-8")).replace("\uFEFF", "").replace("\uEFBBBF", "");
      String[] lines = fileContents.split("\n");
      saveExecutionLog(color("\n\nScript Output: ", White, Bold), INFO);
      for (String line : lines) {
        int index = line.indexOf('=');
        if (index != -1) {
          String key = line.substring(0, index).trim();
          String value = line.substring(index + 1).trim().replace("\r", "");
          envVariablesMap.put(key, value);
          saveExecutionLog(color(key + "=" + value, Gray), INFO);
        }
      }

      cleanupEnvironmentOutputFile(session, envVariablesOutputFile);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      logger.error("Exception while trying to collectOutputEnvironmentVariables", e);
    }
    return envVariablesMap;
  }

  private void cleanupEnvironmentOutputFile(WinRmSession session, String envVariablesOutputFile) {
    String command = "Remove-Item -Path " + envVariablesOutputFile;
    try (StringWriter outputAccumulator = new StringWriter(1024)) {
      session.executeCommandString(psWrappedCommand(command), outputAccumulator, outputAccumulator);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      logger.error("Exception while trying to remove envVariablesOutputFile {}", envVariablesOutputFile, e);
    }
  }

  private String psWrappedCommand(String command) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;
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

  private CommandExecutionStatus downloadConfigFile(ConfigFileMetaData configFileMetaData) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try (WinRmSession session = new WinRmSession(config); ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
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
        logger.error("Error while downloading config file.", e);
      }

      String encodedFile = EncodingUtils.encodeBase64(fileBytes);
      String command = "#### Convert Base64 string back to config file ####\n"
          + "\n"
          + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\""
          + encodedFile + "\"))\n"
          + "Write-Host \"Decoding config file on the host.\"\n"
          + "$decodedFile = \'" + configFileMetaData.getFilename() + "\'\n"
          + "[IO.File]::WriteAllText($decodedFile, $DecodedString) \n"
          + "Write-Host \"Copied config file to the host.\"\n";

      int exitCode = session.executeCommandString(psWrappedCommand(command), outputWriter, errorWriter);
      logger.info("Execute Command String returned exit code.", exitCode);
      commandExecutionStatus = SUCCESS;
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      logger.error("Error while executing command", e);
      ResponseMessage details = buildErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }
    return commandExecutionStatus;
  }

  @Override
  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    CommandExecutionStatus commandExecutionStatus;
    if (isBlank(configFileMetaData.getFileId()) || isBlank(configFileMetaData.getFilename())) {
      saveExecutionLog("There is no config file to copy. " + configFileMetaData.toString(), INFO);
      return CommandExecutionStatus.SUCCESS;
    }
    commandExecutionStatus = downloadConfigFile(configFileMetaData);
    logger.info("Copy Config command execution returned.", commandExecutionStatus);
    return commandExecutionStatus;
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath,
      ArtifactStreamAttributes artifactStreamAttributes, String accountId, String appId, String activityId,
      String commandUnitName, String hostName) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    throw new NotImplementedException("Not implemented");
  }

  private ExecutionLogWriter getExecutionLogWriter(LogLevel logLevel) {
    return ExecutionLogWriter.builder()
        .accountId(config.getAccountId())
        .appId(config.getAppId())
        .commandUnitName(config.getCommandUnitName())
        .executionId(config.getExecutionId())
        .hostName(config.getHostname())
        .logService(logService)
        .stringBuilder(new StringBuilder(1024))
        .logLevel(logLevel)
        .build();
  }
}
