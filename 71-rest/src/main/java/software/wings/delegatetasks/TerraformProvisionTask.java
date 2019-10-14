package software.wings.delegatetasks;

import static com.google.common.base.Joiner.on;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand.APPLY;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformExecutionData.TerraformExecutionDataBuilder;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.GitOperationContext;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

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
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Slf4j
public class TerraformProvisionTask extends AbstractDelegateRunnableTask {
  private static final String USER_DIR_KEY = "user.dir";
  private static final String TERRAFORM_STATE_FILE_NAME = "terraform.tfstate";
  private static final String WORKSPACE_DIR_BASE = "terraform.tfstate.d";
  private static final String WORKSPACE_STATE_FILE_PATH_FORMAT = WORKSPACE_DIR_BASE + "/%s/terraform.tfstate";
  private static final String TERRAFORM_PLAN_FILE_NAME = "terraform.tfplan";
  private static final String TERRAFORM_VARIABLES_FILE_NAME = "terraform-%s.tfvars";
  private static final String TERRAFORM_BACKEND_CONFIGS_FILE_NAME = "backend_configs";
  private static final String TERRAFORM_INTERNAL_FOLDER = ".terraform";
  private static final long RESOURCE_READY_WAIT_TIME_SECONDS = 15;

  @Inject private GitClient gitClient;
  @Inject private GitClientHelper gitClientHelper;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

  public TerraformProvisionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TerraformExecutionData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
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

    writer.write(format("%s = \"%s\"%n", key, value.replaceAll("\"", "\\\"")));
  }

  private String getAllTfVarFilesArgument(String userDir, String gitDir, List<String> tfVarFiles) {
    StringBuffer buffer = new StringBuffer();
    if (isNotEmpty(tfVarFiles)) {
      tfVarFiles.forEach(file -> {
        String pathForFile = Paths.get(userDir, gitDir, file).toString();
        buffer.append(String.format(" -var-file=\"%s\" ", pathForFile));
      });
    }
    return buffer.toString();
  }

  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    GitConfig gitConfig = parameters.getSourceRepo();
    String sourceRepoSettingId = parameters.getSourceRepoSettingId();

    GitOperationContext gitOperationContext =
        GitOperationContext.builder().gitConfig(gitConfig).gitConnectorId(sourceRepoSettingId).build();

    saveExecutionLog(parameters,
        "Branch: " + gitConfig.getBranch() + "\nNormalized Path: " + parameters.getScriptPath(),
        CommandExecutionStatus.RUNNING);
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    if (isNotEmpty(gitConfig.getReference())) {
      saveExecutionLog(parameters, format("Inheriting git state at commit id: [%s]", gitConfig.getReference()),
          CommandExecutionStatus.RUNNING);
    }

    try {
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails());
      gitClient.ensureRepoLocallyClonedAndUpdated(gitOperationContext);
    } catch (RuntimeException ex) {
      logger.error("Exception in processing git operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
    String scriptDirectory = resolveScriptDirectory(gitOperationContext, parameters.getScriptPath());
    logger.info("Script Directory: " + scriptDirectory);

    File tfVariablesFile = null, tfBackendConfigsFile = null;

    try (ActivityLogOutputStream activityLogOutputStream = new ActivityLogOutputStream(parameters);
         PlanLogOutputStream planLogOutputStream = new PlanLogOutputStream(parameters, new ArrayList<>())) {
      ensureLocalCleanup(scriptDirectory);
      String sourceRepoReference = getLatestCommitSHAFromLocalRepo(gitOperationContext);

      tfVariablesFile =
          Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, parameters.getEntityId())).toFile();
      tfBackendConfigsFile = Paths.get(scriptDirectory, TERRAFORM_BACKEND_CONFIGS_FILE_NAME).toFile();

      downloadTfStateFile(parameters, scriptDirectory);

      boolean entityIdTfVarFileCreated = false;
      if (isNotEmpty(parameters.getVariables()) || isNotEmpty(parameters.getEncryptedVariables())) {
        try (BufferedWriter writer =
                 new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tfVariablesFile), "UTF-8"))) {
          if (isNotEmpty(parameters.getVariables())) {
            for (Entry<String, String> entry : parameters.getVariables().entrySet()) {
              saveVariable(writer, entry.getKey(), entry.getValue());
            }
          }

          if (isNotEmpty(parameters.getEncryptedVariables())) {
            for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedVariables().entrySet()) {
              String value = String.valueOf(encryptionService.getDecryptedValue(entry.getValue()));
              saveVariable(writer, entry.getKey(), value);
            }
          }
        }
        entityIdTfVarFileCreated = true;
      } else {
        FileUtils.deleteQuietly(tfVariablesFile);
      }

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
              String value = String.valueOf(encryptionService.getDecryptedValue(entry.getValue()));
              saveVariable(writer, entry.getKey(), value);
            }
          }
        }
      }

      File tfOutputsFile =
          Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, parameters.getEntityId())).toFile();
      String targetArgs = getTargetArgs(parameters.getTargets());
      String tfVarFiles = getAllTfVarFilesArgument(System.getProperty(USER_DIR_KEY),
          gitClientHelper.getRepoDirectory(gitOperationContext), parameters.getTfVarFiles());
      if (entityIdTfVarFileCreated) {
        tfVarFiles = format("%s -var-file=\"%s\"", tfVarFiles, tfVariablesFile.toString());
      }

      int code;
      switch (parameters.getCommand()) {
        case APPLY: {
          String command = format("terraform init %s",
              tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                            : "");
          /**
           * echo "no" is to prevent copying of state from local to remote by suppressing the
           * copy prompt. As of tf version 0.12.3
           * there is no way to provide this as a command line argument
           */
          saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
          code = executeShellCommand(
              format("echo \"no\" | %s", command), scriptDirectory, parameters, activityLogOutputStream);

          if (isNotEmpty(parameters.getWorkspace())) {
            WorkspaceCommand workspaceCommand =
                getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
            command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0) {
            command = format("terraform refresh -input=false %s %s ", targetArgs, tfVarFiles);
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0) {
            command = format("terraform plan -out=tfplan -input=false %s %s ", targetArgs, tfVarFiles);
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, planLogOutputStream);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            command = "terraform apply -input=false tfplan";
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            command = format("terraform output --json > %s", tfOutputsFile.toString());
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }
          break;
        }
        case DESTROY: {
          String command = format("terraform init -input=false %s",
              tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                            : "");
          saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
          code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);

          if (isNotEmpty(parameters.getWorkspace())) {
            WorkspaceCommand workspaceCommand =
                getWorkspaceCommand(scriptDirectory, parameters.getWorkspace(), parameters.getTimeoutInMillis());
            command = format("terraform workspace %s %s", workspaceCommand.command, parameters.getWorkspace());
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }

          if (code == 0) {
            command = format("terraform refresh -input=false %s %s", targetArgs, tfVarFiles);
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0) {
            command = format("terraform destroy -force %s %s", targetArgs, tfVarFiles);
            saveExecutionLog(parameters, command, CommandExecutionStatus.RUNNING);
            code = executeShellCommand(command, scriptDirectory, parameters, activityLogOutputStream);
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
            CommandExecutionStatus.RUNNING);
        sleep(ofSeconds(RESOURCE_READY_WAIT_TIME_SECONDS));
      }

      CommandExecutionStatus commandExecutionStatus =
          code == 0 ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE;

      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

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
      if (APPLY.equals(parameters.getCommand())) {
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

      List<NameValuePair> variableList = new ArrayList<>();
      if (isNotEmpty(parameters.getVariables())) {
        for (Entry<String, String> variable : parameters.getVariables().entrySet()) {
          variableList.add(new NameValuePair(variable.getKey(), variable.getValue(), Type.TEXT.name()));
        }
      }

      if (isNotEmpty(parameters.getEncryptedVariables())) {
        for (Entry<String, EncryptedDataDetail> encVariable : parameters.getEncryptedVariables().entrySet()) {
          variableList.add(new NameValuePair(
              encVariable.getKey(), encVariable.getValue().getEncryptedData().getUuid(), Type.ENCRYPTED_TEXT.name()));
        }
      }

      List<NameValuePair> backendConfigs = new ArrayList<>();
      if (isNotEmpty(parameters.getBackendConfigs())) {
        for (Entry<String, String> entry : parameters.getBackendConfigs().entrySet()) {
          backendConfigs.add(new NameValuePair(entry.getKey(), entry.getValue(), Type.TEXT.name()));
        }
      }

      if (isNotEmpty(parameters.getEncryptedBackendConfigs())) {
        for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedBackendConfigs().entrySet()) {
          backendConfigs.add(new NameValuePair(
              entry.getKey(), entry.getValue().getEncryptedData().getUuid(), Type.ENCRYPTED_TEXT.name()));
        }
      }

      final TerraformExecutionDataBuilder terraformExecutionDataBuilder =
          TerraformExecutionData.builder()
              .entityId(delegateFile.getEntityId())
              .stateFileId(delegateFile.getFileId())
              .planLogFileId(APPLY.equals(parameters.getCommand()) ? planLogFile.getFileId() : null)
              .commandExecuted(parameters.getCommand())
              .sourceRepoReference(sourceRepoReference)
              .variables(variableList)
              .backendConfigs(backendConfigs)
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

    } catch (RuntimeException | IOException | InterruptedException | TimeoutException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      logger.error("Exception in processing terraform operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    } finally {
      FileUtils.deleteQuietly(tfVariablesFile);
      FileUtils.deleteQuietly(tfBackendConfigsFile);
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

  private List<String> getWorkspacesList(String scriptDir, long timeout)
      throws InterruptedException, TimeoutException, IOException {
    String command = "terraform workspace list";
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .timeout(timeout, TimeUnit.MILLISECONDS)
                                          .directory(Paths.get(scriptDir).toFile());

    ProcessResult processResult = processExecutor.execute();
    String output = processResult.outputString();
    if (processResult.getExitValue() != 0) {
      throw new WingsException("Failed to list workspaces. " + output);
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

  private int executeShellCommand(String command, String directory, TerraformProvisionParameters parameters,
      LogOutputStream logOutputStream) throws RuntimeException, IOException, InterruptedException, TimeoutException {
    String joinedCommands = format("cd \"%s\" && %s", directory, command);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(parameters.getTimeoutInMillis(), TimeUnit.MILLISECONDS)
                                          .command("/bin/sh", "-c", joinedCommands)
                                          .readOutput(true)
                                          .redirectOutput(logOutputStream);

    ProcessResult processResult = processExecutor.execute();
    return processResult.getExitValue();
  }

  private void ensureLocalCleanup(String scriptDirectory) throws IOException {
    FileUtils.deleteQuietly(Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile());
    try {
      deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory, TERRAFORM_INTERNAL_FOLDER).toString());
    } catch (IOException e) {
      logger.warn("Failed to delete .terraform folder");
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

  private String getLatestCommitSHAFromLocalRepo(GitOperationContext gitOperationContext) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitOperationContext));
    if (repoDir.exists()) {
      try (Git git = Git.open(repoDir)) {
        Iterator<RevCommit> commits = git.log().call().iterator();
        if (commits.hasNext()) {
          RevCommit firstCommit = commits.next();

          return firstCommit.toString().split(" ")[1];
        }
      } catch (IOException | GitAPIException e) {
        logger.error("Failed to extract the commit id from the cloned repo.");
      }
    }

    return null;
  }

  private String resolveScriptDirectory(GitOperationContext gitOperationContext, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty(USER_DIR_KEY)).toString(),
            gitClientHelper.getRepoDirectory(gitOperationContext), scriptPath == null ? "" : scriptPath)
        .toString();
  }

  private void saveExecutionLog(
      TerraformProvisionParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withCommandUnitName(parameters.getCommandUnit().name())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  private class ActivityLogOutputStream extends LogOutputStream {
    private TerraformProvisionParameters parameters;

    @Override
    protected void processLine(String line) {
      saveExecutionLog(parameters, line, CommandExecutionStatus.RUNNING);
    }
  }

  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  private class PlanLogOutputStream extends LogOutputStream {
    private TerraformProvisionParameters parameters;
    private List<String> logs;

    @Override
    protected void processLine(String line) {
      saveExecutionLog(parameters, line, CommandExecutionStatus.RUNNING);
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
}
