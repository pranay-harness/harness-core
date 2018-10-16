package software.wings.core.local.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.ScriptType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.delegatetasks.DelegateLogService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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

  public CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect) {
    CommandExecutionResult commandExecutionResult = null;
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    saveExecutionLog(format("Executing command ..."), INFO);

    switch (this.scriptType) {
      case BASH:
        try {
          commandExecutionResult = executeBashScript(command, envVariablesToCollect);
        } catch (Exception e) {
          saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
        }
        break;

      case POWERSHELL:
        throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);

      default:
        unhandled(this.scriptType);
    }

    return commandExecutionResult;
  }

  private CommandExecutionResult executeBashScript(String command, List<String> envVariablesToCollect)
      throws IOException {
    ShellExecutionDataBuilder executionDataBuilder = ShellExecutionData.builder();
    CommandExecutionResult.Builder commandExecutionResult = aCommandExecutionResult().but();
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
        outputStream.write(config.getKubeConfigContent().getBytes(Charset.forName("UTF-8")));
        environment.put(HARNESS_KUBE_CONFIG_PATH, kubeConfigFile.getCanonicalPath());
      }
    }

    String envVariablesFilename = null;
    File envVariablesOutputFile = null;
    if (!envVariablesToCollect.isEmpty()) {
      envVariablesFilename = "harness-" + this.config.getExecutionId() + ".out";
      envVariablesOutputFile = new File(workingDirectory, envVariablesFilename);
    }
    Map<String, String> envVariablesMap = new HashMap<>();
    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
      command = addEnvVariablesCollector(command, envVariablesToCollect, envVariablesFilename);
      outputStream.write(command.getBytes(Charset.forName("UTF-8")));

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
      if (commandExecutionStatus == SUCCESS && envVariablesOutputFile != null) {
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(new FileInputStream(envVariablesOutputFile), "UTF-8"))) {
          String sCurrentLine;
          saveExecutionLog("Script output: ", INFO);
          while ((sCurrentLine = br.readLine()) != null) {
            String[] parts = sCurrentLine.split("=");
            envVariablesMap.put(parts[0], parts[1].trim());
            saveExecutionLog(parts[0] + "=" + parts[1].trim(), INFO);
          }

        } catch (IOException e) {
          saveExecutionLog("IOException:" + e, ERROR);
        }
      }
      executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
      saveExecutionLog(
          format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO, commandExecutionStatus);
    } catch (RuntimeException | InterruptedException | TimeoutException e) {
      executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
      saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
    } finally {
      if (isEmpty(config.getWorkingDirectory())) {
        deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      } else {
        deleteFileIfExists(scriptFile.getAbsolutePath());
        deleteFileIfExists(kubeConfigFile.getAbsolutePath());
        if (envVariablesOutputFile != null) {
          deleteFileIfExists(envVariablesOutputFile.getAbsolutePath());
        }
      }
    }
    commandExecutionResult.withStatus(commandExecutionStatus);
    commandExecutionResult.withCommandExecutionData(executionDataBuilder.build());
    return commandExecutionResult.build();
  }

  private String addEnvVariablesCollector(
      String command, List<String> envVariablesToCollect, String envVariablesOutputFileName) {
    StringBuilder wrapperCommand = new StringBuilder(command);
    wrapperCommand.append('\n');
    String redirect = ">";
    for (String env : envVariablesToCollect) {
      wrapperCommand.append("echo $")
          .append(env)
          .append("| xargs echo \"")
          .append(env)
          .append("=\" ")
          .append(redirect)
          .append(envVariablesOutputFileName)
          .append('\n');
      redirect = ">>";
    }
    return wrapperCommand.toString();
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
