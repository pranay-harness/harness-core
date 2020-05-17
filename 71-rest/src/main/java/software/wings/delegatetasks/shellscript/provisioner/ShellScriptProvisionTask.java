package software.wings.delegatetasks.shellscript.provisioner;

import static java.util.Collections.emptyList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class ShellScriptProvisionTask extends AbstractDelegateRunnableTask {
  private static final String PROVISIONER_OUTPUT_PATH_KEY = "PROVISIONER_OUTPUT_PATH";
  @Inject private DelegateLogService logService;
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private EncryptionService encryptionService;

  public ShellScriptProvisionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public ShellScriptProvisionExecutionData run(TaskParameters taskParameters) {
    File outputFile = null;
    try {
      ShellScriptProvisionParameters parameters = (ShellScriptProvisionParameters) taskParameters;
      String outputPath = getOutputPath(parameters.getWorkflowExecutionId());
      saveExecutionLog(parameters,
          "\"" + PROVISIONER_OUTPUT_PATH_KEY + "\" has been initialized to \"" + outputPath + "\"",
          CommandExecutionStatus.RUNNING);

      HashMap<String, String> variablesMap =
          getCombinedVariablesMap(parameters.getTextVariables(), parameters.getEncryptedVariables());
      variablesMap.put(PROVISIONER_OUTPUT_PATH_KEY, outputPath);

      outputFile = createNewFile(outputPath);

      ShellExecutorConfig shellExecutorConfig = ShellExecutorConfig.builder()
                                                    .accountId(parameters.getAccountId())
                                                    .appId(parameters.getAppId())
                                                    .commandUnitName(parameters.getCommandUnit())
                                                    .executionId(parameters.getActivityId())
                                                    .environment(variablesMap)
                                                    .scriptType(ScriptType.BASH)
                                                    .build();
      ScriptProcessExecutor executor = shellExecutorFactory.getExecutor(shellExecutorConfig);
      CommandExecutionResult commandExecutionResult =
          executor.executeCommandString(parameters.getScriptBody(), emptyList());

      saveExecutionLog(parameters, "Execution finished with status: " + commandExecutionResult.getStatus(),
          commandExecutionResult.getStatus());
      if (commandExecutionResult.getStatus() == CommandExecutionStatus.FAILURE) {
        return ShellScriptProvisionExecutionData.builder()
            .executionStatus(ExecutionStatus.FAILED)
            .errorMsg(commandExecutionResult.getErrorMessage())
            .build();
      }

      try {
        return ShellScriptProvisionExecutionData.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .output(new String(Files.readAllBytes(Paths.get(outputPath)), Charsets.UTF_8))
            .build();
      } catch (IOException e) {
        throw new WingsException("Error occurred while reading output file", e);
      }
    } catch (Exception e) {
      logger.error("Error occurred in the task", e);
      return ShellScriptProvisionExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMsg(ExceptionUtils.getMessage(e))
          .build();
    } finally {
      FileUtils.deleteQuietly(outputFile);
    }
  }

  @VisibleForTesting
  HashMap<String, String> getCombinedVariablesMap(
      Map<String, String> textVariables, Map<String, EncryptedDataDetail> encryptedVariables) {
    HashMap<String, String> envMap = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(textVariables)) {
      envMap.putAll(textVariables);
    }
    if (EmptyPredicate.isNotEmpty(encryptedVariables)) {
      for (Entry<String, EncryptedDataDetail> encryptedVariable : encryptedVariables.entrySet()) {
        try {
          envMap.put(encryptedVariable.getKey(),
              String.valueOf(encryptionService.getDecryptedValue(encryptedVariable.getValue())));
        } catch (IOException e) {
          throw new WingsException("Error occurred while decrypting encrypted variables", e);
        }
      }
    }
    return envMap;
  }

  private String getOutputPath(String entityId) {
    return Paths.get("").toAbsolutePath().toString() + "/shellScriptProvisioner/" + entityId + "/output.json";
  }

  private File createNewFile(String path) {
    File file = new File(path);
    boolean mkdirs = file.getParentFile().mkdirs();
    if (!mkdirs && !file.getParentFile().exists()) {
      throw new WingsException("Unable to create directory for output file");
    }
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new WingsException("Error occurred in creating output file", e);
    }
    return file;
  }

  private void saveExecutionLog(
      ShellScriptProvisionParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .withCommandUnitName(parameters.getCommandUnit())
            .build());
  }
}
