package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.TerraformConstants.RESOURCE_READY_WAIT_TIME_SECONDS;
import static io.harness.provision.TerraformConstants.TERRAFORM_BACKEND_CONFIGS_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_DIR;
import static io.harness.provision.TerraformConstants.TF_WORKING_DIR;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.terraform.TerraformHelperUtils;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;

@Slf4j
@OwnedBy(CDP)
public class TerraformApplyTaskHandler extends TerraformAbstractTaskHandler {
  @Inject TerraformBaseHelper terraformBaseHelper;

  @Override
  public TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters, String delegateId,
      String taskId, LogCallback logCallback) throws IOException, TerraformCommandExecutionException {
    GitStoreDelegateConfig confileFileGitStore = taskParameters.getConfigFile().getGitStoreDelegateConfig();
    String scriptPath = confileFileGitStore.getPaths().get(0);

    if (isNotEmpty(confileFileGitStore.getBranch())) {
      logCallback.saveExecutionLog("Branch: " + confileFileGitStore.getBranch(), INFO, CommandExecutionStatus.RUNNING);
    }

    logCallback.saveExecutionLog("Normalized Path: " + scriptPath, INFO, CommandExecutionStatus.RUNNING);

    if (isNotEmpty(confileFileGitStore.getCommitId())) {
      logCallback.saveExecutionLog(
          format("%nInheriting git state at commit id: [%s]", confileFileGitStore.getCommitId()), INFO,
          CommandExecutionStatus.RUNNING);
    }
    GitBaseRequest gitBaseRequestForConfigFile = terraformBaseHelper.getGitBaseRequestForConfigFile(
        taskParameters.getAccountId(), confileFileGitStore, (GitConfigDTO) confileFileGitStore.getGitConfigDTO());

    String baseDir = TF_WORKING_DIR + taskParameters.getEntityId();

    String scriptDirectory = terraformBaseHelper.fetchConfigFileAndPrepareScriptDir(gitBaseRequestForConfigFile,
        taskParameters.getAccountId(), taskParameters.getWorkspace(), taskParameters.getCurrentStateFileId(),
        confileFileGitStore, logCallback, scriptPath, baseDir);

    String tfVarDirectory = Paths.get(baseDir, TF_VAR_FILES_DIR).toString();
    List<String> varFilePaths = terraformBaseHelper.checkoutRemoteVarFileAndConvertToVarFilePaths(
        taskParameters.getVarFileInfos(), scriptDirectory, logCallback, taskParameters.getAccountId(), tfVarDirectory);

    File tfOutputsFile = Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, "output")).toFile();

    try (PlanJsonLogOutputStream planJsonLogOutputStream = new PlanJsonLogOutputStream()) {
      TerraformExecuteStepRequest terraformExecuteStepRequest =
          TerraformExecuteStepRequest.builder()
              .tfBackendConfigsFile(taskParameters.getBackendConfig() != null
                      ? TerraformHelperUtils.createFileFromStringContent(
                          taskParameters.getBackendConfig(), scriptDirectory, TERRAFORM_BACKEND_CONFIGS_FILE_NAME)
                      : taskParameters.getBackendConfig())
              .tfOutputsFile(tfOutputsFile.getAbsolutePath())
              .tfVarFilePaths(varFilePaths)
              .workspace(taskParameters.getWorkspace())
              .targets(taskParameters.getTargets())
              .scriptDirectory(scriptDirectory)
              .encryptedTfPlan(taskParameters.getEncryptedTfPlan())
              .encryptionConfig(taskParameters.getEncryptionConfig())
              .envVars(taskParameters.getEnvironmentVariables())
              .isSaveTerraformJson(taskParameters.isSaveTerraformStateJson())
              .logCallback(logCallback)
              .planJsonLogOutputStream(planJsonLogOutputStream)
              .timeoutInMillis(taskParameters.getTimeoutInMillis())
              .build();

      CliResponse response = terraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);

      logCallback.saveExecutionLog(
          format("Waiting: [%s] seconds for resources to be ready", RESOURCE_READY_WAIT_TIME_SECONDS), INFO,
          CommandExecutionStatus.RUNNING);
      sleep(ofSeconds(RESOURCE_READY_WAIT_TIME_SECONDS));

      logCallback.saveExecutionLog("Script execution finished with status: " + response.getCommandExecutionStatus(),
          INFO, CommandExecutionStatus.RUNNING);

      Map<String, String> commitIdToFetchedFilesMap = terraformBaseHelper.buildcommitIdToFetchedFilesMap(
          taskParameters.getAccountId(), taskParameters.getConfigFile().getIdentifier(), gitBaseRequestForConfigFile,
          taskParameters.getVarFileInfos());

      File tfStateFile = TerraformHelperUtils.getTerraformStateFile(scriptDirectory, taskParameters.getWorkspace());

      String stateFileId = terraformBaseHelper.uploadTfStateFile(
          taskParameters.getAccountId(), delegateId, taskId, taskParameters.getEntityId(), tfStateFile);

      return TerraformTaskNGResponse.builder()
          .outputs(new String(Files.readAllBytes(tfOutputsFile.toPath()), Charsets.UTF_8))
          .commitIdForConfigFilesMap(commitIdToFetchedFilesMap)
          .encryptedTfPlan(taskParameters.getEncryptedTfPlan())
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .stateFileId(stateFileId)
          .build();

    } catch (TerraformCommandExecutionException terraformCommandExecutionException) {
      log.warn("Failed to execute TerraformApplyStep", terraformCommandExecutionException);
      logCallback.saveExecutionLog("Failed", ERROR, CommandExecutionStatus.FAILURE);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(terraformCommandExecutionException))
          .build();
    } catch (Exception exception) {
      log.warn("Exception Occurred", exception);
      logCallback.saveExecutionLog("Failed", ERROR, CommandExecutionStatus.FAILURE);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(exception))
          .build();
    }
  }
}
