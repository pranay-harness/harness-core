package software.wings.core.ssh.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionResultBuilder;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 2019-03-11.
 */
public class ScriptProcessExecutor extends AbstractScriptExecutor {
  private ShellExecutorConfig config;
  private ScriptType scriptType;
  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param delegateFileManager the file service
   * @param logService          the log service
   */
  @Inject
  public ScriptProcessExecutor(DelegateFileManager delegateFileManager, DelegateLogService logService,
      ScriptExecutionContext shellExecutorConfig) {
    super(delegateFileManager, logService);
    this.config = (ShellExecutorConfig) shellExecutorConfig;
    this.scriptType = ((ShellExecutorConfig) shellExecutorConfig).getScriptType();
  }

  private static final String defaultParentWorkingDirectory = "./local-scripts/";
  static {
    try {
      createDirectoryIfDoesNotExist(defaultParentWorkingDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create local-scripts directory", e);
    }
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    File workingDirectory;
    try {
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

      try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
        outputStream.write(command.getBytes(Charset.forName("UTF-8")));

        String[] commandList = new String[] {"/bin/bash", scriptFilename};
        ProcessExecutor processExecutor =
            new ProcessExecutor()
                .command(commandList)
                .directory(workingDirectory)
                .environment(environment)
                .readOutput(true)
                .redirectOutput(new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    saveExecutionLog(line, INFO);
                  }
                })
                .redirectOutputAlsoTo(output != null ? new StringBufferOutputStream(output) : new NullOutputStream())
                .redirectError(new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    saveExecutionLog(line, ERROR);
                  }
                });

        ProcessResult processResult = processExecutor.execute();
        commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
        if (commandExecutionStatus == SUCCESS) {
          saveExecutionLog(format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO,
              commandExecutionStatus);
        } else {
          saveExecutionLog(format("CommandExecution failed with exit code: (%d)", processResult.getExitValue()), ERROR,
              commandExecutionStatus);
        }
      } catch (RuntimeException | InterruptedException | TimeoutException e) {
        saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
      } finally {
        if (isEmpty(config.getWorkingDirectory())) {
          deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
        } else {
          deleteFileIfExists(scriptFile.getAbsolutePath());
          deleteFileIfExists(kubeConfigFile.getAbsolutePath());
        }
      }
    } catch (IOException e) {
      saveExecutionLog("IOException:" + e, ERROR);
    }
    return commandExecutionStatus;
  }

  @SuppressWarnings("PMD.AvoidStringBufferField")
  static class StringBufferOutputStream extends OutputStream {
    protected StringBuffer buffer;

    StringBufferOutputStream(StringBuffer out) {
      buffer = out;
    }

    @Override
    public void write(int ch) {
      buffer.append((char) ch);
    }
  }

  @Override
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
    CommandExecutionResultBuilder commandExecutionResult = CommandExecutionResult.builder();
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

    String envVariablesFilename;
    File envVariablesOutputFile = null;
    if (!envVariablesToCollect.isEmpty()) {
      envVariablesFilename = "harness-" + this.config.getExecutionId() + ".out";
      envVariablesOutputFile = new File(workingDirectory, envVariablesFilename);
      command = addEnvVariablesCollector(command, envVariablesToCollect, envVariablesOutputFile.getAbsolutePath());
    }
    Map<String, String> envVariablesMap = new HashMap<>();
    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
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
                                                saveExecutionLog(line, INFO);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line, ERROR);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      if (commandExecutionStatus == SUCCESS && envVariablesOutputFile != null) {
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(new FileInputStream(envVariablesOutputFile), "UTF-8"))) {
          String sCurrentLine;
          saveExecutionLog("Script Output: ", INFO);
          while ((sCurrentLine = br.readLine()) != null) {
            int index = sCurrentLine.indexOf('=');
            if (index != -1) {
              String key = sCurrentLine.substring(0, index).trim();
              String value = sCurrentLine.substring(index + 1).trim();
              envVariablesMap.put(key, value);
              saveExecutionLog(key + "=" + value, INFO);
            }
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
    commandExecutionResult.status(commandExecutionStatus);
    commandExecutionResult.commandExecutionData(executionDataBuilder.build());
    return commandExecutionResult.build();
  }

  private String addEnvVariablesCollector(
      String command, List<String> envVariablesToCollect, String envVariablesOutputFilePath) {
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
          .append(envVariablesOutputFilePath)
          .append('\n');
      redirect = ">>";
    }
    return wrapperCommand.toString();
  }

  @Override
  public CommandExecutionStatus scpOneFile(String remoteFilePath, FileProvider fileProvider) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    try {
      Pair<String, Long> fileInfo = fileProvider.getInfo();
      OutputStream out = new FileOutputStream(remoteFilePath + "/" + fileInfo.getKey());
      fileProvider.downloadToStream(out);
      out.flush();
      out.close();
      commandExecutionStatus = SUCCESS;
      saveExecutionLog("File successfully downloaded to " + remoteFilePath);
    } catch (ExecutionException | IOException e) {
      logger.error("Command execution failed with error", e);
    }
    return commandExecutionStatus;
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

  @Override
  public String getAccountId() {
    return config.getAccountId();
  }

  @Override
  public String getCommandUnitName() {
    return config.getCommandUnitName();
  }

  @Override
  public String getAppId() {
    return config.getAppId();
  }

  @Override
  public String getExecutionId() {
    return config.getExecutionId();
  }

  @Override
  public String getHost() {
    return null;
  }
}
