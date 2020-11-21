package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.TerraformConstants.RESOURCE_READY_WAIT_TIME_SECONDS;
import static io.harness.provision.TerraformConstants.TERRAFORM_APPLY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_BACKEND_CONFIGS_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_INTERNAL_FOLDER;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_BASE_DIR;
import static io.harness.provision.TerraformConstants.TF_SCRIPT_DIR;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_DIR;
import static io.harness.provision.TerraformConstants.USER_DIR_KEY;
import static io.harness.provision.TerraformConstants.VAR_FILE_FORMAT;
import static io.harness.provision.TerraformConstants.WORKSPACE_DESTROY_PLAN_FILE_PATH_FORMAT;
import static io.harness.provision.TerraformConstants.WORKSPACE_DIR_BASE;
import static io.harness.provision.TerraformConstants.WORKSPACE_PLAN_FILE_PATH_FORMAT;
import static io.harness.provision.TerraformConstants.WORKSPACE_STATE_FILE_PATH_FORMAT;
import static io.harness.provision.TfVarSource.TfVarSourceType;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand.APPLY;
import static software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand.DESTROY;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitRepositoryType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;

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
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
public class TerraformProvisionTask extends AbstractDelegateRunnableTask {
  @Inject private GitClient gitClient;
  @Inject private GitClientHelper gitClientHelper;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

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

  private String getAllTfVarFilesArgument(String userDir, String gitDir, List<String> tfVarFiles) {
    StringBuffer buffer = new StringBuffer();
    if (isNotEmpty(tfVarFiles)) {
      tfVarFiles.forEach(file -> {
        String pathForFile = Paths.get(userDir, gitDir, file).toString();
        buffer.append(String.format(VAR_FILE_FORMAT, pathForFile));
      });
    }
    return buffer.toString();
  }

  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    GitConfig gitConfig = parameters.getSourceRepo();
    String sourceRepoSettingId = parameters.getSourceRepoSettingId();

    GitOperationContext gitOperationContext =
        GitOperationContext.builder().gitConfig(gitConfig).gitConnectorId(sourceRepoSettingId).build();

    if (isNotEmpty(gitConfig.getBranch())) {
      saveExecutionLog(parameters, "Branch: " + gitConfig.getBranch(), CommandExecutionStatus.RUNNING, INFO);
    }
    saveExecutionLog(
        parameters, "\nNormalized Path: " + parameters.getScriptPath(), CommandExecutionStatus.RUNNING, INFO);
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    if (isNotEmpty(gitConfig.getReference())) {
      saveExecutionLog(parameters, format("%nInheriting git state at commit id: [%s]", gitConfig.getReference()),
          CommandExecutionStatus.RUNNING, INFO);
    }

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
      fetchTfVarGitSource(parameters, tfVarDirectory);
    }

    try {
      copyFilesToWorkingDirectory(gitClientHelper.getRepoDirectory(gitOperationContext), workingDir);
    } catch (Exception ex) {
      log.error("Exception in copying files to provisioner specific directory", ex);
      FileUtils.deleteQuietly(new File(workingDir));
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
    String scriptDirectory = resolveScriptDirectory(workingDir, parameters.getScriptPath());
    log.info("Script Directory: " + scriptDirectory);
    saveExecutionLog(
        parameters, format("Script Directory: [%s]", scriptDirectory), CommandExecutionStatus.RUNNING, INFO);

    File tfVariablesFile = null, tfBackendConfigsFile = null;

    try (ActivityLogOutputStream activityLogOutputStream = new ActivityLogOutputStream(parameters);
         PlanLogOutputStream planLogOutputStream = new PlanLogOutputStream(parameters, new ArrayList<>());
         PlanJsonLogOutputStream planJsonLogOutputStream = new PlanJsonLogOutputStream();) {
      ensureLocalCleanup(scriptDirectory);
      String sourceRepoReference = parameters.getCommitId() != null
          ? parameters.getCommitId()
          : getLatestCommitSHAFromLocalRepo(gitOperationContext);
      final Map<String, String> envVars = getEnvironmentVariables(parameters);
      saveExecutionLog(parameters, format("Environment variables: [%s]", collectEnvVarKeys(envVars)),
          CommandExecutionStatus.RUNNING, INFO);

      tfVariablesFile =
          Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, parameters.getEntityId())).toFile();
      tfBackendConfigsFile =
          Paths.get(scriptDirectory, format(TERRAFORM_BACKEND_CONFIGS_FILE_NAME, parameters.getEntityId())).toFile();

      downloadTfStateFile(parameters, scriptDirectory);

      StringBuilder inlineCommandBuffer = new StringBuilder();
      StringBuilder inlineUILogBuffer = new StringBuilder();
      getCommandLineVariableParams(parameters, tfVariablesFile, inlineCommandBuffer, inlineUILogBuffer);
      String varParams = inlineCommandBuffer.toString();
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
      String tfVarFiles =
          getAllTfVarFilesArgument(System.getProperty(USER_DIR_KEY), workingDir, parameters.getTfVarFiles());
      varParams = format("%s %s", tfVarFiles, varParams);
      uiLogs = format("%s %s", tfVarFiles, uiLogs);

      int code;
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
          saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
          code = executeShellCommand(
              format("echo \"no\" | %s", command), scriptDirectory, parameters, envVars, activityLogOutputStream);

          if (isNotEmpty(parameters.getWorkspace())) {
            WorkspaceCommand workspaceCommand =
                getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
            command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
            commandToLog = command;
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }
          if (code == 0 && !shouldSkipRefresh(parameters)) {
            command = format("terraform refresh -input=false %s %s ", targetArgs, varParams);
            commandToLog = format("terraform refresh -input=false %s %s ", targetArgs, uiLogs);
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }
          // if the plan exists we should use the approved plan, instead create a plan
          if (code == 0 && parameters.getTerraformPlan() == null) {
            command = format("terraform plan -out=tfplan -input=false %s %s ", targetArgs, varParams);
            commandToLog = format("terraform plan -out=tfplan -input=false %s %s ", targetArgs, uiLogs);
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, planLogOutputStream);

            if (code == 0 && parameters.isSaveTerraformJson()) {
              code = executeTerraformShowCommand(parameters, scriptDirectory, APPLY, envVars, planJsonLogOutputStream);
            }
          } else if (code == 0 && parameters.getTerraformPlan() != null) {
            // case when we are inheriting the approved  plan
            saveTerraformPlanContentToFile(parameters, scriptDirectory);
            saveExecutionLog(parameters, color("\nUsing approved terraform plan \n", LogColor.Yellow, LogWeight.Bold),
                CommandExecutionStatus.RUNNING, INFO);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            command = "terraform apply -input=false tfplan";
            commandToLog = command;
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            command = format("terraform output --json > %s", tfOutputsFile.toString());
            commandToLog = command;
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }

          break;
        }
        case DESTROY: {
          String command = format("terraform init -input=false %s",
              tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                            : "");
          String commandToLog = command;
          saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
          code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);

          if (isNotEmpty(parameters.getWorkspace())) {
            WorkspaceCommand workspaceCommand =
                getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
            command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
            commandToLog = command;
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }

          if (code == 0 && !shouldSkipRefresh(parameters)) {
            command = format("terraform refresh -input=false %s %s", targetArgs, varParams);
            commandToLog = format("terraform refresh -input=false %s %s", targetArgs, uiLogs);
            saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
            code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
          }
          if (code == 0) {
            if (parameters.isRunPlanOnly()) {
              command = format("terraform plan -destroy -out=tfdestroyplan -input=false %s %s ", targetArgs, varParams);
              commandToLog =
                  format("terraform plan -destroy -out=tfdestroyplan -input=false %s %s ", targetArgs, uiLogs);
              saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
              code = executeShellCommand(command, scriptDirectory, parameters, envVars, planLogOutputStream);

              if (code == 0 && parameters.isSaveTerraformJson()) {
                code =
                    executeTerraformShowCommand(parameters, scriptDirectory, DESTROY, envVars, planJsonLogOutputStream);
              }
            } else {
              if (parameters.getTerraformPlan() == null) {
                command = format("terraform destroy -force %s %s", targetArgs, varParams);
                commandToLog = format("terraform destroy -force %s %s", targetArgs, uiLogs);
                saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
                code = executeShellCommand(command, scriptDirectory, parameters, envVars, activityLogOutputStream);
              } else {
                // case when we are inheriting the approved destroy plan
                saveTerraformPlanContentToFile(parameters, scriptDirectory);
                saveExecutionLog(
                    parameters, "Using approved terraform destroy plan", CommandExecutionStatus.RUNNING, INFO);

                command = "terraform apply -input=false tfdestroyplan";
                commandToLog = command;
                saveExecutionLog(parameters, commandToLog, CommandExecutionStatus.RUNNING, INFO);
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

      if (code == 0 && !parameters.isRunPlanOnly()) {
        saveExecutionLog(parameters,
            format("Waiting: [%s] seconds for resources to be ready", String.valueOf(RESOURCE_READY_WAIT_TIME_SECONDS)),
            CommandExecutionStatus.RUNNING, INFO);
        sleep(ofSeconds(RESOURCE_READY_WAIT_TIME_SECONDS));
      }

      CommandExecutionStatus commandExecutionStatus =
          code == 0 ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE;

      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus, INFO);

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

      DelegateFile planLogFile = null;
      if (APPLY == parameters.getCommand()) {
        planLogFile = aDelegateFile()
                          .withAccountId(parameters.getAccountId())
                          .withDelegateId(getDelegateId())
                          .withTaskId(getTaskId())
                          .withEntityId(parameters.getEntityId())
                          .withBucket(FileBucket.TERRAFORM_PLAN)
                          .withFileName(TERRAFORM_PLAN_FILE_NAME)
                          .build();
        planLogFile = delegateFileManager.upload(
            planLogFile, new ByteArrayInputStream(planLogOutputStream.getPlanLog().getBytes(StandardCharsets.UTF_8)));
      }

      List<NameValuePair> backendConfigs =
          getAllVariables(parameters.getBackendConfigs(), parameters.getEncryptedBackendConfigs());
      List<NameValuePair> environmentVars =
          getAllVariables(parameters.getEnvironmentVariables(), parameters.getEncryptedEnvironmentVariables());

      byte[] terraformPlan = parameters.isRunPlanOnly() && parameters.isExportPlanToApplyStep()
          ? getTerraformPlanFile(scriptDirectory, parameters)
          : null;

      final TerraformExecutionDataBuilder terraformExecutionDataBuilder =
          TerraformExecutionData.builder()
              .entityId(delegateFile.getEntityId())
              .stateFileId(delegateFile.getFileId())
              .planLogFileId(APPLY == parameters.getCommand() ? planLogFile.getFileId() : null)
              .tfPlanFile(terraformPlan)
              .tfPlanJson(planJsonLogOutputStream.getPlanJson())
              .commandExecuted(parameters.getCommand())
              .sourceRepoReference(sourceRepoReference)
              .variables(parameters.getRawVariables())
              .backendConfigs(backendConfigs)
              .environmentVariables(environmentVars)
              .targets(parameters.getTargets())
              .tfVarFiles(parameters.getTfVarFiles())
              .delegateTag(parameters.getDelegateTag())
              .executionStatus(code == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
              .errorMessage(code == 0 ? null : "The terraform command exited with code " + code)
              .workspace(parameters.getWorkspace());

      if (parameters.getCommandUnit() != TerraformCommandUnit.Destroy
          && commandExecutionStatus == CommandExecutionStatus.SUCCESS && !parameters.isRunPlanOnly()) {
        terraformExecutionDataBuilder.outputs(new String(Files.readAllBytes(tfOutputsFile.toPath()), Charsets.UTF_8));
      }

      return terraformExecutionDataBuilder.build();

    } catch (WingsException ex) {
      return logErrorAndGetFailureResponse(parameters, ex, ExceptionUtils.getMessage(ex));
    } catch (IOException ex) {
      return logErrorAndGetFailureResponse(parameters, ex, "IO Failure occurred while performing Terraform Task");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return logErrorAndGetFailureResponse(parameters, ex, "Interrupted while performing Terraform Task");
    } catch (TimeoutException | UncheckedTimeoutException ex) {
      return logErrorAndGetFailureResponse(parameters, ex, "Timed out while performing Terraform Task");
    } catch (Exception ex) {
      return logErrorAndGetFailureResponse(parameters, ex, "Failed to complete Terraform Task");
    } finally {
      FileUtils.deleteQuietly(tfVariablesFile);
      FileUtils.deleteQuietly(tfBackendConfigsFile);
      FileUtils.deleteQuietly(new File(workingDir));
    }
  }

  private void fetchTfVarGitSource(TerraformProvisionParameters parameters, String tfVarDirectory) {
    if (parameters.getTfVarSource().getTfVarSourceType() == TfVarSourceType.GIT) {
      TfVarGitSource tfVarGitSource = (TfVarGitSource) parameters.getTfVarSource();
      saveExecutionLog(parameters,
          format("Fetching TfVar files from Git repository: [%s]", tfVarGitSource.getGitConfig().getRepoUrl()),
          CommandExecutionStatus.RUNNING, INFO);

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
          parameters, format("TfVar Git directory: [%s]", tfVarDirectory), CommandExecutionStatus.RUNNING, INFO);
    }
  }

  private boolean shouldSkipRefresh(TerraformProvisionParameters parameters) {
    return parameters.getTerraformPlan() != null && parameters.isSkipRefreshBeforeApplyingPlan();
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
      TerraformProvisionParameters.TerraformCommand terraformCommand, Map<String, String> envVars,
      PlanJsonLogOutputStream planJsonLogOutputStream) throws IOException, InterruptedException, TimeoutException {
    String planName =
        terraformCommand == APPLY ? TERRAFORM_PLAN_FILE_OUTPUT_NAME : TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
    saveExecutionLog(parameters, format("%nGenerating json representation of %s %n", planName),
        CommandExecutionStatus.RUNNING, INFO);
    String command = format("terraform show -json %s", planName);
    saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING, INFO);
    int code = executeShellCommand(command, scriptDirectory, parameters, envVars, planJsonLogOutputStream);
    if (code == 0) {
      saveExecutionLog(parameters,
          format("%nJson representation of %s is exported as a variable %s %n", planName,
              terraformCommand == APPLY ? TERRAFORM_APPLY_PLAN_FILE_VAR_NAME : TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME),
          CommandExecutionStatus.RUNNING, INFO);
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

  private TerraformExecutionData logErrorAndGetFailureResponse(
      TerraformProvisionParameters parameters, Exception ex, String message) {
    saveExecutionLog(parameters, message, CommandExecutionStatus.FAILURE, ERROR);
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
    File tfStateFile = (isEmpty(parameters.getWorkspace()))
        ? Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile()
        : Paths.get(scriptDirectory, format(WORKSPACE_STATE_FILE_PATH_FORMAT, parameters.getWorkspace())).toFile();

    if (parameters.getCurrentStateFileId() != null) {
      try (InputStream stateRemoteInputStream = delegateFileManager.downloadByFileId(
               FileBucket.TERRAFORM_STATE, parameters.getCurrentStateFileId(), parameters.getAccountId())) {
        FileUtils.copyInputStreamToFile(stateRemoteInputStream, tfStateFile);
      }
    } else {
      FileUtils.deleteQuietly(tfStateFile);
    }
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
    return parseOutput(output);
  }

  @VisibleForTesting
  public List<String> parseOutput(String workspaceOutput) {
    List<String> outputs = Arrays.asList(StringUtils.split(workspaceOutput, "\n"));
    List<String> workspaces = new ArrayList<>();
    for (String output : outputs) {
      if (output.charAt(0) == '*') {
        output = output.substring(1);
      }
      output = output.trim();
      workspaces.add(output);
    }
    return workspaces;
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
    return isEmpty(parameters.getWorkspace())
        ? Files.readAllBytes(Paths.get(scriptDirectory, getPlanName(parameters)))
        : Files.readAllBytes(Paths.get(scriptDirectory,
            getWorkspacePlanFileFormat(parameters).replace("$WORKSPACE_NAME", parameters.getWorkspace())));
  }

  @VisibleForTesting
  public void saveTerraformPlanContentToFile(TerraformProvisionParameters parameters, String scriptDirectory)
      throws IOException {
    File tfPlanFile = isEmpty(parameters.getWorkspace())
        ? Paths.get(scriptDirectory, getPlanName(parameters)).toFile()
        : Paths
              .get(scriptDirectory,
                  getWorkspacePlanFileFormat(parameters).replace("$WORKSPACE_NAME", parameters.getWorkspace()))
              .toFile();

    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(parameters.getTerraformPlan()), tfPlanFile);
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

  @NotNull
  private String getWorkspacePlanFileFormat(TerraformProvisionParameters parameters) {
    switch (parameters.getCommand()) {
      case APPLY:
        return WORKSPACE_PLAN_FILE_PATH_FORMAT;
      case DESTROY:
        return WORKSPACE_DESTROY_PLAN_FILE_PATH_FORMAT;
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

  private void saveExecutionLog(TerraformProvisionParameters parameters, String line,
      CommandExecutionStatus commandExecutionStatus, LogLevel logLevel) {
    logService.save(parameters.getAccountId(),
        aLog()
            .appId(parameters.getAppId())
            .activityId(parameters.getActivityId())
            .logLevel(logLevel)
            .commandUnitName(parameters.getCommandUnit().name())
            .logLine(line)
            .executionResult(commandExecutionStatus)
            .build());
  }

  private String color(String line, LogColor color, LogWeight logWeight) {
    return LogHelper.doneColoring(LogHelper.color(line, color, logWeight));
  }

  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  private class ActivityLogOutputStream extends LogOutputStream {
    private TerraformProvisionParameters parameters;

    @Override
    protected void processLine(String line) {
      saveExecutionLog(parameters, line, CommandExecutionStatus.RUNNING, INFO);
    }
  }

  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  private class PlanLogOutputStream extends LogOutputStream {
    private TerraformProvisionParameters parameters;
    private List<String> logs;

    @Override
    protected void processLine(String line) {
      saveExecutionLog(parameters, line, CommandExecutionStatus.RUNNING, INFO);
      if (logs == null) {
        logs = new ArrayList<>();
      }
      logs.add(line);
    }

    String getPlanLog() {
      if (isNotEmpty(logs)) {
        return on("\n").join(logs);
      }
      return "";
    }
  }

  @EqualsAndHashCode(callSuper = false)
  private class PlanJsonLogOutputStream extends LogOutputStream {
    private String planJson;

    @Override
    protected void processLine(String line) {
      planJson = line;
    }

    protected String getPlanJson() {
      return planJson;
    }
  }
}
