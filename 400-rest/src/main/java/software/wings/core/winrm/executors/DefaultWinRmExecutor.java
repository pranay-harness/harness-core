package software.wings.core.winrm.executors;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.utils.WinRmHelperUtils.buildErrorDetailsFromWinRmClientException;

import static java.lang.String.format;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionResultBuilder;
import io.harness.eraro.ResponseMessage;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.core.ssh.executors.WinRmExecutorHelper;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.utils.ExecutionLogWriter;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class DefaultWinRmExecutor implements WinRmExecutor {
  public static final String HARNESS_FILENAME_PREFIX = "\\harness-";
  public static final String WINDOWS_TEMPFILE_LOCATION = "%TEMP%";
  public static final String NOT_IMPLEMENTED = "Not implemented";
  private static final String ERROR_WHILE_EXECUTING_COMMAND = "Error while executing command";
  public static final String POWERSHELL_NO_PROFILE = "Powershell -NoProfile ";
  public static final String POWERSHELL = "Powershell ";
  protected DelegateLogService logService;
  private final WinRmSessionConfig config;
  protected DelegateFileManager delegateFileManager;
  private final boolean disableCommandEncoding;
  private final boolean shouldSaveExecutionLogs;
  private final String powershell;

  DefaultWinRmExecutor(DelegateLogService logService, DelegateFileManager delegateFileManager,
      boolean shouldSaveExecutionLogs, WinRmSessionConfig config, boolean disableCommandEncoding) {
    this.logService = logService;
    this.delegateFileManager = delegateFileManager;
    this.shouldSaveExecutionLogs = shouldSaveExecutionLogs;
    this.config = config;
    this.disableCommandEncoding = disableCommandEncoding;

    if (this.config.isUseNoProfile()) {
      powershell = POWERSHELL_NO_PROFILE;
    } else {
      powershell = POWERSHELL;
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
            WinRmExecutorHelper.constructPSScriptWithCommands(command, psScriptFile, powershell), outputWriter,
            errorWriter, false);
      } else {
        exitCode = session.executeCommandString(
            WinRmExecutorHelper.psWrappedCommandWithEncoding(command, powershell), outputWriter, errorWriter, false);
      }
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;
      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
      WinRmExecutorHelper.cleanupFiles(session, psScriptFile, powershell, disableCommandEncoding);
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
            WinRmExecutorHelper.constructPSScriptWithCommands(command, psScriptFile, powershell), outputWriter,
            errorWriter, false);
      } else {
        exitCode = session.executeCommandString(
            WinRmExecutorHelper.psWrappedCommandWithEncoding(command, powershell), outputWriter, errorWriter, false);
      }
      commandExecutionStatus = (exitCode == 0) ? SUCCESS : FAILURE;

      if (commandExecutionStatus == SUCCESS && envVariablesOutputFile != null) {
        // If we are here, we will run another command to get the output variables. Make sure we delete the previous
        // script
        WinRmExecutorHelper.cleanupFiles(session, psScriptFile, powershell, disableCommandEncoding);
        executionDataBuilder.sweepingOutputEnvVariables(
            collectOutputEnvironmentVariables(session, envVariablesOutputFile));
      }

      saveExecutionLog(format("%nCommand completed with ExitCode (%d)", exitCode), INFO, commandExecutionStatus);
      commandExecutionResult.status(commandExecutionStatus);
      commandExecutionResult.commandExecutionData(executionDataBuilder.build());
      WinRmExecutorHelper.cleanupFiles(session, psScriptFile, powershell, disableCommandEncoding);
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
        exitCode = session.executeCommandsList(
            WinRmExecutorHelper.constructPSScriptWithCommands(commandWithoutEncoding, psScriptFile, powershell),
            outputAccumulator, errorAccumulator, true);
      } else {
        exitCode = session.executeCommandString(WinRmExecutorHelper.psWrappedCommandWithEncoding(command, powershell),
            outputAccumulator, errorAccumulator, true);
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

      WinRmExecutorHelper.cleanupFiles(session, envVariablesOutputFile, powershell, disableCommandEncoding);
      WinRmExecutorHelper.cleanupFiles(session, psScriptFile, powershell, disableCommandEncoding);
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      log.error("Exception while trying to collectOutputEnvironmentVariables", e);
    }
    return envVariablesMap;
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
