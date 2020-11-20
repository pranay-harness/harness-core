package software.wings.core.winrm.executors;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.utils.WinRmHelperUtils.buildErrorDetailsFromWinRmClientException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionResultBuilder;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.eraro.ResponseMessage;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.utils.ExecutionLogWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class DefaultWinRmExecutor implements WinRmExecutor {
  public static final String HARNESS_FILENAME_PREFIX = "\\harness-";
  public static final String WINDOWS_TEMPFILE_LOCATION = "%TEMP%";
  public static final String NOT_IMPLEMENTED = "Not implemented";
  private static final String ERROR_WHILE_EXECUTING_COMMAND = "Error while executing command";
  protected DelegateLogService logService;
  private final WinRmSessionConfig config;
  protected DelegateFileManager delegateFileManager;
  private boolean disableCommandEncoding;
  private boolean shouldSaveExecutionLogs;
  private String powershell = "Powershell ";
  private static final int SPLITLISTOFCOMMANDSBY = 20;

  DefaultWinRmExecutor(DelegateLogService logService, DelegateFileManager delegateFileManager,
      boolean shouldSaveExecutionLogs, WinRmSessionConfig config, boolean disableCommandEncoding) {
    this.logService = logService;
    this.delegateFileManager = delegateFileManager;
    this.shouldSaveExecutionLogs = shouldSaveExecutionLogs;
    this.config = config;
    this.disableCommandEncoding = disableCommandEncoding;

    if (this.config.isUseNoProfile()) {
      powershell = "Powershell -NoProfile ";
    }
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
    ExecutionLogCallback executionLogCallback = getExecutionLogCallback(config.getCommandUnitName());

    try (WinRmSession session = new WinRmSession(config, executionLogCallback);
         ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      if (displayCommand) {
        saveExecutionLog(format("Executing command %s...", command), INFO);
      } else {
        saveExecutionLog(format("Executing command ...%n"), INFO);
      }

      int exitCode;
      String psScriptFile = null;
      if (disableCommandEncoding) {
        psScriptFile = getPSScriptFile();
        exitCode = session.executeCommandsList(
            constructPSScriptWithCommands(command, psScriptFile), outputWriter, errorWriter, false);
      } else {
        exitCode =
            session.executeCommandString(psWrappedCommandWithEncoding(command), outputWriter, errorWriter, false);
      }
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;
      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
      cleanupFiles(session, psScriptFile);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      log.error(ERROR_WHILE_EXECUTING_COMMAND, e);
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
          .append("\n Write-Output $e | Out-File -Encoding UTF8 -append -FilePath '")
          .append(envVariablesOutputFileName)
          .append("'\n");
    }
    return wrapperCommand.toString();
  }

  /*
  Keep the temp script in working directory or in Temp is working directory is not set.
   */
  @NotNull
  private String getPSScriptFile() {
    return config.getWorkingDirectory() == null
        ? WINDOWS_TEMPFILE_LOCATION
        : config.getWorkingDirectory() + HARNESS_FILENAME_PREFIX + this.config.getExecutionId() + ".ps1";
  }

  @Override
  public CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect) {
    ShellExecutionDataBuilder executionDataBuilder = ShellExecutionData.builder();
    CommandExecutionResultBuilder commandExecutionResult = CommandExecutionResult.builder();
    CommandExecutionStatus commandExecutionStatus;
    ExecutionLogCallback executionLogCallback = getExecutionLogCallback(config.getCommandUnitName());

    saveExecutionLog(format("Initializing WinRM connection to %s ...", config.getHostname()), INFO);
    String envVariablesOutputFile = null;
    String psScriptFile = null;

    if (!envVariablesToCollect.isEmpty()) {
      envVariablesOutputFile = this.config.getWorkingDirectory() + "\\"
          + "harness-" + this.config.getExecutionId() + ".out";
    }

    try (WinRmSession session = new WinRmSession(config, executionLogCallback);
         ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      saveExecutionLog(format("Executing command ...%n"), INFO);

      command = addEnvVariablesCollector(command, envVariablesToCollect, envVariablesOutputFile);
      int exitCode;
      if (disableCommandEncoding) {
        psScriptFile = getPSScriptFile();

        exitCode = session.executeCommandsList(
            constructPSScriptWithCommands(command, psScriptFile), outputWriter, errorWriter, false);
      } else {
        exitCode =
            session.executeCommandString(psWrappedCommandWithEncoding(command), outputWriter, errorWriter, false);
      }
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;

      if (commandExecutionStatus == SUCCESS && envVariablesOutputFile != null) {
        // If we are here, we will run another command to get the output variables. Make sure we delete the previous
        // script
        cleanupFiles(session, psScriptFile);
        executionDataBuilder.sweepingOutputEnvVariables(
            collectOutputEnvironmentVariables(session, envVariablesOutputFile));
      }

      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
      commandExecutionResult.status(commandExecutionStatus);
      commandExecutionResult.commandExecutionData(executionDataBuilder.build());
      cleanupFiles(session, psScriptFile);
      return commandExecutionResult.build();
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      commandExecutionStatus = FAILURE;
      log.error(ERROR_WHILE_EXECUTING_COMMAND, e);
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

    // Note we are already receving file contents in UTF-8
    String commandWithoutEncoding = "$envVarString = Get-Content -Path \"" + envVariablesOutputFile
        + "\" -Encoding UTF8 -Raw; Write-Host $envVarString -NoNewline ";

    String psScriptFile = null;

    try (StringWriter outputAccumulator = new StringWriter(1024);
         StringWriter errorAccumulator = new StringWriter(1024)) {
      int exitCode;
      if (disableCommandEncoding) {
        psScriptFile = getPSScriptFile();
        exitCode = session.executeCommandsList(constructPSScriptWithCommands(commandWithoutEncoding, psScriptFile),
            outputAccumulator, errorAccumulator, true);
      } else {
        exitCode = session.executeCommandString(
            psWrappedCommandWithEncoding(command), outputAccumulator, errorAccumulator, true);
      }
      if (exitCode != 0) {
        log.error("Powershell script collecting output environment Variables failed with exitCode {}", exitCode);
        return envVariablesMap;
      }

      String fileContents;
      if (disableCommandEncoding) {
        fileContents = outputAccumulator.toString();
      } else {
        byte[] decodedBytes = Base64.getDecoder().decode(outputAccumulator.getBuffer().toString());
        // removing UTF-8 and UTF-16 BOMs if any
        fileContents = new String(decodedBytes, StandardCharsets.UTF_8).replace("\uFEFF", "").replace("\uEFBBBF", "");
      }
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

      cleanupFiles(session, envVariablesOutputFile);
      cleanupFiles(session, psScriptFile);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      log.error("Exception while trying to collectOutputEnvironmentVariables", e);
    }
    return envVariablesMap;
  }

  @VisibleForTesting
  protected void cleanupFiles(WinRmSession session, String file) {
    if (file == null) {
      return;
    }

    String command = "Remove-Item -Path '" + file + "'";
    try (StringWriter outputAccumulator = new StringWriter(1024)) {
      if (disableCommandEncoding) {
        command = format(
            "%s Invoke-Command -command {$FILE_PATH=[System.Environment]::ExpandEnvironmentVariables(\\\"%s\\\") ;  Remove-Item -Path $FILE_PATH}",
            powershell, file);
        session.executeCommandString(command, outputAccumulator, outputAccumulator, false);
      } else {
        session.executeCommandString(
            psWrappedCommandWithEncoding(command), outputAccumulator, outputAccumulator, false);
      }
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      log.error("Exception while trying to remove file {} {}", file, e);
    }
  }

  /**
   * To construct the powershell script for running on target windows host.
   * @param command  Command String
   * @return Parsed command after escaping special characters. Command will write a powershell script file and then
   * execute it. Due to character limit for single powershell command, the command is split at a new line character and
   * writes one line at a time.
   */
  protected List<List<String>> constructPSScriptWithCommands(String command, String psScriptFile) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;

    // Yes, replace() is intentional. We are replacing only character and not a regex pattern.
    command = command.replace("$", "`$");
    // This is to escape quotes
    command = command.replaceAll("\"", "`\\\\\"");

    // write commands to a file and then execute the file
    String appendPSInvokeCommandtoCommandString;
    appendPSInvokeCommandtoCommandString =
        powershell + " Invoke-Command -command {[IO.File]::AppendAllText(\\\"%s\\\", \\\"%s\\\" ) }";
    // Split the command at newline character
    List<String> listofCommands = Arrays.asList(command.split("\n"));

    // Replace pipe only if part of a string, else skip
    Pattern patternForPipeWithinAString = Pattern.compile("[a-zA-Z]+\\|");
    List<String> commandList = new ArrayList<>();
    for (String commandString : listofCommands) {
      if (patternForPipeWithinAString.matcher(commandString).find()) {
        commandString = commandString.replaceAll("\\|", "`\\\"|`\\\"");
      }
      // Append each command with PS Invoke command which is write command to file and also add the PS newline character
      // for correct escaping
      commandList.add(format(appendPSInvokeCommandtoCommandString, psScriptFile, commandString + "`r`n"));
    }
    // last command to run the script we just built - This will execute our command.
    commandList.add(format("%s -f \"%s\" ", powershell, psScriptFile));
    return Lists.partition(commandList, SPLITLISTOFCOMMANDSBY);
  }

  /**
   * Constructs powershell command by encoding the command string to base64 command.
   * @param command Command String
   * @return powershell command string that will convert that command from base64 to UTF8 string on windows host and
   * then run it on cmd.
   */
  @VisibleForTesting
  String psWrappedCommandWithEncoding(String command) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return format("%s Invoke-Command -command {%s}", powershell, wrappedCommand);
  }

  private void saveExecutionLog(String line, LogLevel level) {
    saveExecutionLog(line, level, RUNNING);
  }

  private void saveExecutionLog(String line, LogLevel level, CommandExecutionStatus commandExecutionStatus) {
    if (shouldSaveExecutionLogs) {
      logService.save(config.getAccountId(),
          aLog()
              .appId(config.getAppId())
              .activityId(config.getExecutionId())
              .logLevel(level)
              .commandUnitName(config.getCommandUnitName())
              .hostName(config.getHostname())
              .logLine(line)
              .executionResult(commandExecutionStatus)
              .build());
    }
  }

  public ExecutionLogCallback getExecutionLogCallback(String commandUnit) {
    return new ExecutionLogCallback(
        logService, config.getAccountId(), config.getAppId(), config.getExecutionId(), commandUnit);
  }

  private CommandExecutionStatus downloadConfigFile(ConfigFileMetaData configFileMetaData) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    ExecutionLogCallback executionLogCallback = getExecutionLogCallback(config.getCommandUnitName());

    try (WinRmSession session = new WinRmSession(config, executionLogCallback);
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
        command = "$fileName = \"" + configFileMetaData.getFilename() + "\"\n"
            + "$commandString = {" + new String(fileBytes) + "}"
            + "\n[IO.File]::WriteAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
            + "Write-Host \"Copied config file to the host.\"\n";
        exitCode = session.executeCommandsList(
            constructPSScriptWithCommands(command, psScriptFile), outputWriter, errorWriter, false);
      } else {
        String encodedFile = EncodingUtils.encodeBase64(fileBytes);
        command = "#### Convert Base64 string back to config file ####\n"
            + "\n"
            + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\""
            + encodedFile + "\"))\n"
            + "Write-Host \"Decoding config file on the host.\"\n"
            + "$decodedFile = \'" + configFileMetaData.getFilename() + "\'\n"
            + "[IO.File]::WriteAllText($decodedFile, $DecodedString) \n"
            + "Write-Host \"Copied config file to the host.\"\n";

        exitCode =
            session.executeCommandString(psWrappedCommandWithEncoding(command), outputWriter, errorWriter, false);
      }
      log.info("Execute Command String returned exit code.", exitCode);
      commandExecutionStatus = SUCCESS;
      cleanupFiles(session, psScriptFile);
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

  @Override
  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
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
  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    throw new NotImplementedException(NOT_IMPLEMENTED);
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