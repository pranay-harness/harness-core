package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.delegate.task.terraform.TerraformCommand.APPLY;
import static io.harness.delegate.task.terraform.TerraformCommand.DESTROY;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.logging.LogLevel.WARN;
import static io.harness.provision.TerraformConstants.RESOURCE_READY_WAIT_TIME_SECONDS;
import static io.harness.provision.TerraformConstants.TERRAFORM_APPLY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_BACKEND_CONFIGS_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_INTERNAL_FOLDER;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_BASE_DIR;
import static io.harness.provision.TerraformConstants.TF_SCRIPT_DIR;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_DIR;
import static io.harness.provision.TerraformConstants.USER_DIR_KEY;
import static io.harness.provision.TerraformConstants.WORKSPACE_DIR_BASE;
import static io.harness.provision.TerraformConstants.WORKSPACE_STATE_FILE_PATH_FORMAT;
import static io.harness.provision.TfVarSource.TfVarSourceType;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.delegatetasks.validation.terraform.TerraformTaskUtils.fetchAllTfVarFilesArgument;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.cli.CliResponse;
import io.harness.cli.LogCallbackOutputStream;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformCommand;
import io.harness.delegate.task.terraform.TerraformCommandUnit;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitRepositoryType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.secretmanagerclient.EncryptDecryptHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformExecutionData.TerraformExecutionDataBuilder;
import software.wings.api.terraform.TfVarGitSource;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;
import software.wings.beans.LogWeight;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.delegatetasks.validation.terraform.TerraformTaskUtils;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class TerraformProvisionTask extends AbstractDelegateRunnableTask {
  @Inject private GitClient gitClient;
  @Inject private GitClientHelper gitClientHelper;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;
  @Inject private EncryptDecryptHelper planEncryptDecryptHelper;
  @Inject private TerraformBaseHelper terraformBaseHelper;

  public TerraformProvisionTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public TerraformExecutionData run(TaskParameters parameters) {
    return run((TerraformProvisionParameters) parameters);
  }

  @Override
  public TerraformExecutionData run(Object[] parameters) {
    return run((TerraformProvisionParameters) parameters[0]);
  }

  private enum WorkspaceCommand {
    SELECT("select"),
    NEW("new");
    private String command;

    WorkspaceCommand(String command) {
      this.command = command;
    }
  }

  private Pattern varList = Pattern.compile("^\\s*\\[.*?]\\s*$");

  private void saveVariable(BufferedWriter writer, String key, String value) throws IOException {
    // If the variable is wrapped with [] square brackets, we assume it is a list and we keep it as is.
    if (varList.matcher(value).matches()) {
      writer.write(format("%s = %s%n", key, value));
      return;
    }

    writer.write(format("%s = \"%s\" %n", key, value.replaceAll("\"", "\\\"")));
  }

  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    GitConfig gitConfig = parameters.getSourceRepo();
    String sourceRepoSettingId = parameters.getSourceRepoSettingId();
    LogCallback logCallback = getLogCallback(parameters);

    GitOperationContext gitOperationContext =
        GitOperationContext.builder().gitConfig(gitConfig).gitConnectorId(sourceRepoSettingId).build();

    if (isNotEmpty(gitConfig.getBranch())) {
      saveExecutionLog("Branch: " + gitConfig.getBranch(), CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
    saveExecutionLog(
        "\nNormalized Path: " + parameters.getScriptPath(), CommandExecutionStatus.RUNNING, INFO, logCallback);
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    if (isNotEmpty(gitConfig.getReference())) {
      saveExecutionLog(format("%nInheriting git state at commit id: [%s]", gitConfig.getReference()),
          CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
    EncryptedRecordData encryptedTfPlan = parameters.getEncryptedTfPlan();
    try {
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails(), false);
      gitClient.ensureRepoLocallyClonedAndUpdated(gitOperationContext);
    } catch (RuntimeException ex) {
      log.error("Exception in processing git operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(TerraformTaskUtils.getGitExceptionMessageIfExists(ex))
          .build();
    }

    String baseDir = resolveBaseDir(parameters.getAccountId(), parameters.getEntityId());
    String tfVarDirectory = Paths.get(baseDir, TF_VAR_FILES_DIR).toString();
    String workingDir = Paths.get(baseDir, TF_SCRIPT_DIR).toString();

    if (null != parameters.getTfVarSource()
        && parameters.getTfVarSource().getTfVarSourceType() == TfVarSourceType.GIT) {
      fetchTfVarGitSource(parameters, tfVarDirectory, logCallback);
    }

    try {
      copyFilesToWorkingDirectory(gitClientHelper.getRepoDirectory(gitOperationContext), workingDir);
    } catch (Exception ex) {
      log.error("Exception in copying files to provisioner specific directory", ex);
      FileUtils.deleteQuietly(new File(baseDir));
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
    String scriptDirectory = resolveScriptDirectory(workingDir, parameters.getScriptPath());
    log.info("Script Directory: " + scriptDirectory);
    saveExecutionLog(
        format("Script Directory: [%s]", scriptDirectory), CommandExecutionStatus.RUNNING, INFO, logCallback);

    File tfVariablesFile = null, tfBackendConfigsFile = null;

    try (ActivityLogOutputStream activityLogOutputStream = new ActivityLogOutputStream(parameters, logCallback);
         LogCallbackOutputStream logCallbackOutputStream = new LogCallbackOutputStream(logCallback);
         PlanJsonLogOutputStream planJsonLogOutputStream = new PlanJsonLogOutputStream()) {
      ensureLocalCleanup(scriptDirectory);
      String sourceRepoReference = parameters.getCommitId() != null
          ? parameters.getCommitId()
          : getLatestCommitSHAFromLocalRepo(gitOperationContext);
      final Map<String, String> envVars = getEnvironmentVariables(parameters);
      saveExecutionLog(format("Environment variables: [%s]", collectEnvVarKeys(envVars)),
          CommandExecutionStatus.RUNNING, INFO, logCallback);

      tfVariablesFile =
          Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, parameters.getEntityId())).toFile();
      tfBackendConfigsFile =
          Paths.get(scriptDirectory, format(TERRAFORM_BACKEND_CONFIGS_FILE_NAME, parameters.getEntityId())).toFile();

      downloadTfStateFile(parameters, scriptDirectory);

      StringBuilder inlineCommandBuffer = new StringBuilder();
      StringBuilder inlineUILogBuffer = new StringBuilder();
      getCommandLineVariableParams(parameters, tfVariablesFile, inlineCommandBuffer, inlineUILogBuffer);
      String varParams = inlineCommandBuffer.toString();
      String inlineVarParams = varParams;
      String uiLogs = inlineUILogBuffer.toString();

      if (isNotEmpty(parameters.getBackendConfigs()) || isNotEmpty(parameters.getEncryptedBackendConfigs())) {
        try (BufferedWriter writer =
                 new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tfBackendConfigsFile), "UTF-8"))) {
          if (isNotEmpty(parameters.getBackendConfigs())) {
            for (Entry<String, String> entry : parameters.getBackendConfigs().entrySet()) {
              saveVariable(writer, entry.getKey(), entry.getValue());
            }
          }
          if (isNotEmpty(parameters.getEncryptedBackendConfigs())) {
            for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedBackendConfigs().entrySet()) {
              String value = String.valueOf(encryptionService.getDecryptedValue(entry.getValue(), false));
              saveVariable(writer, entry.getKey(), value);
            }
          }
        }
      }

      File tfOutputsFile =
          Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, parameters.getEntityId())).toFile();
      String targetArgs = getTargetArgs(parameters.getTargets());

      String tfVarFiles = null == parameters.getTfVarSource()
          ? StringUtils.EMPTY
          : fetchAllTfVarFilesArgument(
              System.getProperty(USER_DIR_KEY), parameters.getTfVarSource(), workingDir, tfVarDirectory);
      varParams = format("%s %s", tfVarFiles, varParams);
      uiLogs = format("%s %s", tfVarFiles, uiLogs);

      int code;
      if (parameters.isUseTfClient()) {
        try {
          log.info(format("Using TFClient for Running Terraform Commands for account %s", parameters.getAccountId()));
          code = executeWithTerraformClient(parameters, tfBackendConfigsFile, tfOutputsFile, scriptDirectory,
              workingDir, tfVarDirectory, inlineVarParams, uiLogs, envVars, logCallback, planJsonLogOutputStream);
        } catch (TerraformCommandExecutionException exception) {
          log.warn(exception.getMessage());
          code = 0;
        }
      } else {
        switch (parameters.getCommand()) {
          case APPLY: {
            String command = format("terraform init %s",
                tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                              : "");
            String commandToLog = command;
            /**
             * echo "no" is to prevent copying of state from local to remote by suppressing the
             * copy prompt. As of tf version 0.12.3
             * there is no way to provide this as a command line argument
             */
            saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
            code = executeShellCommand(
                format("echo \"no\" | %s", command), scriptDirectory, parameters, envVars, activityLogOutputStream);

            if (isNotEmpty(parameters.getWorkspace())) {
              WorkspaceCommand workspaceCommand =
                  getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
              command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
              commandToLog = command;
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }
            if (code == 0 && !shouldSkipRefresh(parameters)) {
              command = format("terraform refresh -input=false %s %s ", targetArgs, varParams);
              commandToLog = format("terraform refresh -input=false %s %s ", targetArgs, uiLogs);
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }
            // if the plan exists we should use the approved plan, instead create a plan
            if (code == 0 && parameters.getEncryptedTfPlan() == null) {
              saveExecutionLog(color("\nGenerating terraform plan \n", LogColor.Yellow, LogWeight.Bold),
                  CommandExecutionStatus.RUNNING, INFO, logCallback);
              command = format("terraform plan -out=tfplan -input=false %s %s ", targetArgs, varParams);
              commandToLog = format("terraform plan -out=tfplan -input=false %s %s ", targetArgs, uiLogs);
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, logCallbackOutputStream);

              if (code == 0 && parameters.isSaveTerraformJson()) {
                code = executeTerraformShowCommand(
                    parameters, scriptDirectory, APPLY, envVars, planJsonLogOutputStream, logCallback);
              }
            } else if (code == 0 && parameters.getEncryptedTfPlan() != null) {
              // case when we are inheriting the approved  plan
              saveExecutionLog(color("\nDecrypting terraform plan before applying\n", LogColor.Yellow, LogWeight.Bold),
                  CommandExecutionStatus.RUNNING, INFO, logCallback);
              saveTerraformPlanContentToFile(parameters, scriptDirectory);
              saveExecutionLog(color("\nUsing approved terraform plan \n", LogColor.Yellow, LogWeight.Bold),
                  CommandExecutionStatus.RUNNING, INFO, logCallback);
            }
            if (code == 0 && !parameters.isRunPlanOnly()) {
              command = "terraform apply -input=false tfplan";
              commandToLog = command;
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }
            if (code == 0 && !parameters.isRunPlanOnly()) {
              command = format("terraform output --json > %s", tfOutputsFile.toString());
              commandToLog = command;
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }

            break;
          }
          case DESTROY: {
            String command = format("terraform init -input=false %s",
                tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                              : "");
            String commandToLog = command;
            saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);

            if (isNotEmpty(parameters.getWorkspace())) {
              WorkspaceCommand workspaceCommand =
                  getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
              command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
              commandToLog = command;
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }

            if (code == 0 && !shouldSkipRefresh(parameters)) {
              command = format("terraform refresh -input=false %s %s", targetArgs, varParams);
              commandToLog = format("terraform refresh -input=false %s %s", targetArgs, uiLogs);
              saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
            }
            if (code == 0) {
              if (parameters.isRunPlanOnly()) {
                command =
                    format("terraform plan -destroy -out=tfdestroyplan -input=false %s %s ", targetArgs, varParams);
                commandToLog =
                    format("terraform plan -destroy -out=tfdestroyplan -input=false %s %s ", targetArgs, uiLogs);
                saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
                code = executeShellCommand(command, scriptDirectory, parameters, envVars, logCallbackOutputStream);

                if (code == 0 && parameters.isSaveTerraformJson()) {
                  code = executeTerraformShowCommand(
                      parameters, scriptDirectory, DESTROY, envVars, planJsonLogOutputStream, logCallback);
                }
              } else {
                if (parameters.getEncryptedTfPlan() == null) {
                  command = format("terraform destroy -force %s %s", targetArgs, varParams);
                  commandToLog = format("terraform destroy -force %s %s", targetArgs, uiLogs);
                  saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
                  code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
                } else {
                  // case when we are inheriting the approved destroy plan
                  saveTerraformPlanContentToFile(parameters, scriptDirectory);
                  saveExecutionLog(
                      "Using approved terraform destroy plan", CommandExecutionStatus.RUNNING, INFO, logCallback);

                  command = "terraform apply -input=false tfdestroyplan";
                  commandToLog = command;
                  saveExecutionLog(commandToLog, CommandExecutionStatus.RUNNING, INFO, logCallback);
                  code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
                }
              }
            }
            break;
          }
          default: {
            throw new IllegalArgumentException("Invalid Terraform Command : " + parameters.getCommand().name());
          }
        }
      }

      if (code == 0 && !parameters.isRunPlanOnly()) {
        saveExecutionLog(
            format("Waiting: [%s] seconds for resources to be ready", String.valueOf(RESOURCE_READY_WAIT_TIME_SECONDS)),
            CommandExecutionStatus.RUNNING, INFO, logCallback);
        sleep(ofSeconds(RESOURCE_READY_WAIT_TIME_SECONDS));
      }

      CommandExecutionStatus commandExecutionStatus =
          code == 0 ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE;

      saveExecutionLog("Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus, INFO,
          logCallback);

      final DelegateFile delegateFile = aDelegateFile()
                                            .withAccountId(parameters.getAccountId())
                                            .withDelegateId(getDelegateId())
                                            .withTaskId(getTaskId())
                                            .withEntityId(parameters.getEntityId())
                                            .withBucket(FileBucket.TERRAFORM_STATE)
                                            .withFileName(TERRAFORM_STATE_FILE_NAME)
                                            .build();

      File tfStateFile = getTerraformStateFile(scriptDirectory, parameters.getWorkspace());
      if (tfStateFile != null) {
        try (InputStream initialStream = new FileInputStream(tfStateFile)) {
          delegateFileManager.upload(delegateFile, initialStream);
        }
      } else {
        try (InputStream nullInputStream = new NullInputStream(0)) {
          delegateFileManager.upload(delegateFile, nullInputStream);
        }
      }

      List<NameValuePair> backendConfigs =
          getAllVariables(parameters.getBackendConfigs(), parameters.getEncryptedBackendConfigs());
      List<NameValuePair> environmentVars =
          getAllVariables(parameters.getEnvironmentVariables(), parameters.getEncryptedEnvironmentVariables());

      if (parameters.isExportPlanToApplyStep()) {
        byte[] terraformPlanFile = getTerraformPlanFile(scriptDirectory, parameters);
        saveExecutionLog(color("\nEncrypting terraform plan \n", LogColor.Yellow, LogWeight.Bold),
            CommandExecutionStatus.RUNNING, INFO, logCallback);
        encryptedTfPlan = (EncryptedRecordData) planEncryptDecryptHelper.encryptContent(
            terraformPlanFile, parameters.getPlanName(), parameters.getSecretManagerConfig());
      }

      final TerraformExecutionDataBuilder terraformExecutionDataBuilder =
          TerraformExecutionData.builder()
              .entityId(delegateFile.getEntityId())
              .stateFileId(delegateFile.getFileId())
              .tfPlanJson(planJsonLogOutputStream.getPlanJson())
              .commandExecuted(parameters.getCommand())
              .sourceRepoReference(sourceRepoReference)
              .variables(parameters.getRawVariables())
              .backendConfigs(backendConfigs)
              .environmentVariables(environmentVars)
              .targets(parameters.getTargets())
              .tfVarFiles(parameters.getTfVarFiles())
              .tfVarSource(parameters.getTfVarSource())
              .delegateTag(parameters.getDelegateTag())
              .executionStatus(code == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
              .errorMessage(code == 0 ? null : "The terraform command exited with code " + code)
              .workspace(parameters.getWorkspace())
              .encryptedTfPlan(encryptedTfPlan);

      if (parameters.getCommandUnit() != TerraformCommandUnit.Destroy
          && commandExecutionStatus == CommandExecutionStatus.SUCCESS && !parameters.isRunPlanOnly()) {
        terraformExecutionDataBuilder.outputs(new String(Files.readAllBytes(tfOutputsFile.toPath()), Charsets.UTF_8));
      }

      return terraformExecutionDataBuilder.build();

    } catch (WingsException ex) {
      return logErrorAndGetFailureResponse(ex, ExceptionUtils.getMessage(ex), logCallback);
    } catch (IOException ex) {
      return logErrorAndGetFailureResponse(ex, "IO Failure occurred while performing Terraform Task", logCallback);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return logErrorAndGetFailureResponse(ex, "Interrupted while performing Terraform Task", logCallback);
    } catch (TimeoutException | UncheckedTimeoutException ex) {
      return logErrorAndGetFailureResponse(ex, "Timed out while performing Terraform Task", logCallback);
    } catch (Exception ex) {
      return logErrorAndGetFailureResponse(ex, "Failed to complete Terraform Task", logCallback);
    } finally {
      FileUtils.deleteQuietly(new File(workingDir));
      FileUtils.deleteQuietly(new File(baseDir));
      if (parameters.getEncryptedTfPlan() != null) {
        try {
          boolean isSafelyDeleted = planEncryptDecryptHelper.deleteEncryptedRecord(
              parameters.getSecretManagerConfig(), parameters.getEncryptedTfPlan());
          if (isSafelyDeleted) {
            log.info("Terraform Plan has been safely deleted from vault");
          }
        } catch (Exception ex) {
          saveExecutionLog(color(format("Failed to delete secret: [%s] from vault: [%s], please clean it up",
                                     parameters.getEncryptedTfPlan().getEncryptionKey(),
                                     parameters.getSecretManagerConfig().getName()),
                               LogColor.Yellow, LogWeight.Bold),
              CommandExecutionStatus.RUNNING, WARN, logCallback);
          saveExecutionLog(ex.getMessage(), CommandExecutionStatus.RUNNING, WARN, logCallback);
          log.error("Exception occurred while deleting Terraform Plan from vault", ex);
        }
      }
    }
  }

  private int executeWithTerraformClient(TerraformProvisionParameters parameters, File tfBackendConfigsFile,
      File tfOutputsFile, String scriptDirectory, String workingDir, String tfVarDirectory, String varParams,
      String uiLogs, Map<String, String> envVars, LogCallback logCallback,
      PlanJsonLogOutputStream planJsonLogOutputStream)
      throws InterruptedException, IOException, TimeoutException, TerraformCommandExecutionException {
    CliResponse response;

    TerraformExecuteStepRequest terraformExecuteStepRequest =
        TerraformExecuteStepRequest.builder()
            .tfBackendConfigsFile(tfBackendConfigsFile.getAbsolutePath())
            .tfOutputsFile(tfOutputsFile.getAbsolutePath())
            .tfVarFilePaths(TerraformTaskUtils.fetchAndBuildAllTfVarFilesPaths(
                System.getProperty(USER_DIR_KEY), parameters.getTfVarSource(), workingDir, tfVarDirectory))
            .varParams(varParams)
            .uiLogs(uiLogs)
            .scriptDirectory(scriptDirectory)
            .envVars(envVars)
            .targets(parameters.getTargets())
            .workspace(parameters.getWorkspace())
            .isRunPlanOnly(parameters.isRunPlanOnly())
            .encryptedTfPlan(parameters.getEncryptedTfPlan())
            .encryptionConfig(parameters.getSecretManagerConfig())
            .isSkipRefreshBeforeApplyingPlan(parameters.isSkipRefreshBeforeApplyingPlan())
            .isSaveTerraformJson(parameters.isSaveTerraformJson())
            .logCallback(logCallback)
            .planJsonLogOutputStream(planJsonLogOutputStream)
            .build();
    switch (parameters.getCommand()) {
      case APPLY: {
        if (terraformExecuteStepRequest.isRunPlanOnly()) {
          response = terraformBaseHelper.executeTerraformPlanStep(terraformExecuteStepRequest);
        } else {
          response = terraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);
        }
        break;
      }
      case DESTROY: {
        response = terraformBaseHelper.executeTerraformDestroyStep(terraformExecuteStepRequest);
        break;
      }
      default: {
        throw new IllegalArgumentException(
            "Invalid Terraform Command for TF client: " + parameters.getCommand().name());
      }
    }
    return response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS ? 0 : 1;
  }

  private void fetchTfVarGitSource(
      TerraformProvisionParameters parameters, String tfVarDirectory, LogCallback logCallback) {
    if (parameters.getTfVarSource().getTfVarSourceType() == TfVarSourceType.GIT) {
      TfVarGitSource tfVarGitSource = (TfVarGitSource) parameters.getTfVarSource();
      saveExecutionLog(
          format("Fetching TfVar files from Git repository: [%s]", tfVarGitSource.getGitConfig().getRepoUrl()),
          CommandExecutionStatus.RUNNING, INFO, logCallback);

      encryptionService.decrypt(tfVarGitSource.getGitConfig(), tfVarGitSource.getEncryptedDataDetails(), false);
      gitClient.downloadFiles(tfVarGitSource.getGitConfig(),
          GitFetchFilesRequest.builder()
              .branch(tfVarGitSource.getGitFileConfig().getBranch())
              .commitId(tfVarGitSource.getGitFileConfig().getCommitId())
              .filePaths(tfVarGitSource.getGitFileConfig().getFilePathList())
              .useBranch(tfVarGitSource.getGitFileConfig().isUseBranch())
              .gitConnectorId(tfVarGitSource.getGitFileConfig().getConnectorId())
              .recursive(true)
              .build(),
          tfVarDirectory);

      saveExecutionLog(
          format("TfVar Git directory: [%s]", tfVarDirectory), CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
  }

  private boolean shouldSkipRefresh(TerraformProvisionParameters parameters) {
    return parameters.getEncryptedTfPlan() != null && parameters.isSkipRefreshBeforeApplyingPlan();
  }

  private String collectEnvVarKeys(Map<String, String> envVars) {
    if (isNotEmpty(envVars)) {
      return envVars.keySet().stream().collect(Collectors.joining(", "));
    }
    return "";
  }

  private List<NameValuePair> getAllVariables(
      Map<String, String> variables, Map<String, EncryptedDataDetail> encryptedVariables) {
    List<NameValuePair> allVars = new ArrayList<>();
    if (isNotEmpty(variables)) {
      for (Entry<String, String> entry : variables.entrySet()) {
        allVars.add(new NameValuePair(entry.getKey(), entry.getValue(), Type.TEXT.name()));
      }
    }

    if (isNotEmpty(encryptedVariables)) {
      for (Entry<String, EncryptedDataDetail> entry : encryptedVariables.entrySet()) {
        allVars.add(new NameValuePair(
            entry.getKey(), entry.getValue().getEncryptedData().getUuid(), Type.ENCRYPTED_TEXT.name()));
      }
    }
    return allVars;
  }

  private int executeTerraformShowCommand(TerraformProvisionParameters parameters, String scriptDirectory,
      TerraformCommand terraformCommand, Map<String, String> envVars, PlanJsonLogOutputStream planJsonLogOutputStream,
      LogCallback logCallback) throws IOException, InterruptedException, TimeoutException {
    String planName =
        terraformCommand == APPLY ? TERRAFORM_PLAN_FILE_OUTPUT_NAME : TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
    saveExecutionLog(format("%nGenerating json representation of %s %n", planName), CommandExecutionStatus.RUNNING,
        INFO, logCallback);
    String command = format("terraform show -json %s", planName);
    saveExecutionLog(command, CommandExecutionStatus.RUNNING, INFO, logCallback);
    int code = executeShellCommand(command, scriptDirectory, parameters, envVars, planJsonLogOutputStream);
    if (code == 0) {
      saveExecutionLog(
          format("%nJson representation of %s is exported as a variable %s %n", planName,
              terraformCommand == APPLY ? TERRAFORM_APPLY_PLAN_FILE_VAR_NAME : TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME),
          CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
    return code;
  }

  private ImmutableMap<String, String> getEnvironmentVariables(TerraformProvisionParameters parameters)
      throws IOException {
    ImmutableMap.Builder<String, String> envVars = ImmutableMap.builder();
    if (isNotEmpty(parameters.getEnvironmentVariables())) {
      envVars.putAll(parameters.getEnvironmentVariables());
    }
    if (isNotEmpty(parameters.getEncryptedEnvironmentVariables())) {
      for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedEnvironmentVariables().entrySet()) {
        String value = String.valueOf(encryptionService.getDecryptedValue(entry.getValue(), false));
        envVars.put(entry.getKey(), value);
      }
    }
    return envVars.build();
  }

  private TerraformExecutionData logErrorAndGetFailureResponse(Exception ex, String message, LogCallback logCallback) {
    saveExecutionLog(message, CommandExecutionStatus.FAILURE, ERROR, logCallback);
    log.error("Exception in processing terraform operation", ex);
    return TerraformExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(message).build();
  }

  /*
  Copies Files from the directory common to the git connector to a directory specific to the app
  and provisioner
   */
  private void copyFilesToWorkingDirectory(String sourceDir, String destinationDir) throws IOException {
    File dest = new File(destinationDir);
    File src = new File(sourceDir);
    FileUtils.deleteDirectory(dest);
    FileUtils.copyDirectory(src, dest);
    FileIo.waitForDirectoryToBeAccessibleOutOfProcess(dest.getPath(), 10);
  }

  @NonNull
  private String resolveBaseDir(String accountId, String entityId) {
    return TF_BASE_DIR.replace("${ACCOUNT_ID}", accountId).replace("${ENTITY_ID}", entityId);
  }

  @VisibleForTesting
  public void getCommandLineVariableParams(TerraformProvisionParameters parameters, File tfVariablesFile,
      StringBuilder executeParams, StringBuilder uiLogParams) throws IOException {
    if (isEmpty(parameters.getVariables()) && isEmpty(parameters.getEncryptedVariables())) {
      FileUtils.deleteQuietly(tfVariablesFile);
      return;
    }
    String variableFormatString = " -var='%s=%s' ";
    if (isNotEmpty(parameters.getVariables())) {
      for (Entry<String, String> entry : parameters.getVariables().entrySet()) {
        executeParams.append(format(variableFormatString, entry.getKey(), entry.getValue()));
        uiLogParams.append(format(variableFormatString, entry.getKey(), entry.getValue()));
      }
    }

    if (isNotEmpty(parameters.getEncryptedVariables())) {
      for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedVariables().entrySet()) {
        executeParams.append(format(variableFormatString, entry.getKey(),
            String.valueOf(encryptionService.getDecryptedValue(entry.getValue(), false))));
        uiLogParams.append(format(variableFormatString, entry.getKey(), format("HarnessSecret:[%s]", entry.getKey())));
      }
    }
  }

  private void downloadTfStateFile(TerraformProvisionParameters parameters, String scriptDirectory) throws IOException {
    terraformBaseHelper.downloadTfStateFile(
        parameters.getWorkspace(), parameters.getAccountId(), parameters.getCurrentStateFileId(), scriptDirectory);
  }

  private WorkspaceCommand getWorkspaceCommand(String scriptDir, String workspace, long timeoutInMillis)
      throws InterruptedException, IOException, TimeoutException {
    List<String> workspaces = getWorkspacesList(scriptDir, timeoutInMillis);
    return workspaces.contains(workspace) ? WorkspaceCommand.SELECT : WorkspaceCommand.NEW;
  }

  public List<String> getWorkspacesList(String scriptDir, long timeout)
      throws InterruptedException, TimeoutException, IOException {
    String command = "terraform workspace list";
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .timeout(timeout, TimeUnit.MILLISECONDS)
                                          .directory(Paths.get(scriptDir).toFile());

    ProcessResult processResult = processExecutor.execute();
    String output = processResult.outputUTF8();
    if (processResult.getExitValue() != 0) {
      throw new InvalidRequestException("Failed to list workspaces. " + output);
    }
    return terraformBaseHelper.parseOutput(output);
  }

  public int executeShellCommand(String command, String directory, TerraformProvisionParameters parameters,
      Map<String, String> envVars, LogOutputStream logOutputStream)
      throws RuntimeException, IOException, InterruptedException, TimeoutException {
    String joinedCommands = format("cd \"%s\" && %s", directory, command);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(parameters.getTimeoutInMillis(), TimeUnit.MILLISECONDS)
                                          .command("/bin/sh", "-c", joinedCommands)
                                          .readOutput(true)
                                          .environment(envVars)
                                          .redirectOutput(logOutputStream);

    ProcessResult processResult = processExecutor.execute();
    return processResult.getExitValue();
  }

  private void ensureLocalCleanup(String scriptDirectory) throws IOException {
    FileUtils.deleteQuietly(Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile());
    try {
      deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory, TERRAFORM_INTERNAL_FOLDER).toString());
    } catch (IOException e) {
      log.warn("Failed to delete .terraform folder");
    }
    deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory, WORKSPACE_DIR_BASE).toString());
  }

  @VisibleForTesting
  public String getTargetArgs(List<String> targets) {
    StringBuilder targetArgs = new StringBuilder();
    if (isNotEmpty(targets)) {
      for (String target : targets) {
        targetArgs.append("-target=" + target + " ");
      }
    }
    return targetArgs.toString();
  }

  private File getTerraformStateFile(String scriptDirectory, String workspace) {
    File tfStateFile = isEmpty(workspace)
        ? Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile()
        : Paths.get(scriptDirectory, format(WORKSPACE_STATE_FILE_PATH_FORMAT, workspace)).toFile();

    if (tfStateFile.exists()) {
      return tfStateFile;
    }

    return null;
  }

  @VisibleForTesting
  public byte[] getTerraformPlanFile(String scriptDirectory, TerraformProvisionParameters parameters)
      throws IOException {
    return Files.readAllBytes(Paths.get(scriptDirectory, getPlanName(parameters)));
  }

  @VisibleForTesting
  public void saveTerraformPlanContentToFile(TerraformProvisionParameters parameters, String scriptDirectory)
      throws IOException {
    File tfPlanFile = Paths.get(scriptDirectory, getPlanName(parameters)).toFile();

    byte[] decryptedTerraformPlan = planEncryptDecryptHelper.getDecryptedContent(
        parameters.getSecretManagerConfig(), parameters.getEncryptedTfPlan());

    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(decryptedTerraformPlan), tfPlanFile);
  }

  @NotNull
  private String getPlanName(TerraformProvisionParameters parameters) {
    switch (parameters.getCommand()) {
      case APPLY:
        return TERRAFORM_PLAN_FILE_OUTPUT_NAME;
      case DESTROY:
        return TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
      default:
        throw new IllegalArgumentException("Invalid Terraform Command : " + parameters.getCommand().name());
    }
  }

  public String getLatestCommitSHAFromLocalRepo(GitOperationContext gitOperationContext) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitOperationContext));
    if (repoDir.exists()) {
      try (Git git = Git.open(repoDir)) {
        Iterator<RevCommit> commits = git.log().call().iterator();
        if (commits.hasNext()) {
          RevCommit firstCommit = commits.next();

          return firstCommit.toString().split(" ")[1];
        }
      } catch (IOException | GitAPIException e) {
        log.error("Failed to extract the commit id from the cloned repo.");
      }
    }

    return null;
  }

  private String resolveScriptDirectory(String workingDir, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty(USER_DIR_KEY)).toString(), workingDir, scriptPath == null ? "" : scriptPath)
        .toString();
  }

  private void saveExecutionLog(
      String line, CommandExecutionStatus commandExecutionStatus, LogLevel logLevel, LogCallback logCallback) {
    logCallback.saveExecutionLog(line, logLevel, commandExecutionStatus);
  }

  private LogCallback getLogCallback(TerraformProvisionParameters parameters) {
    return new ExecutionLogCallback(logService, parameters.getAccountId(), parameters.getAppId(),
        parameters.getActivityId(), parameters.getCommandUnit().name());
  }

  private String color(String line, LogColor color, LogWeight logWeight) {
    return LogHelper.doneColoring(LogHelper.color(line, color, logWeight));
  }

  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  private class ActivityLogOutputStream extends LogOutputStream {
    private TerraformProvisionParameters parameters;
    private LogCallback logCallback;

    @Override
    protected void processLine(String line) {
      saveExecutionLog(line, CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
  }
}