package software.wings.delegatetasks;

import static com.google.common.base.Joiner.on;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
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

import com.bertramlabs.plugins.hcl4j.HCLParser;
import com.bertramlabs.plugins.hcl4j.HCLParserException;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformExecutionData.TerraformExecutionDataBuilder;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.security.encryption.EncryptedDataDetail;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class TerraformProvisionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TerraformProvisionTask.class);
  private static final String TERRAFORM_STATE_FILE_NAME = "terraform.tfstate";
  private static final String TERRAFORM_PLAN_FILE_NAME = "terraform.tfplan";
  private static final String TERRAFORM_VARIABLES_FILE_NAME = "terraform.tfvars";
  private static final String TERRAFORM_SCRIPT_FILE_EXTENSION = "tf";
  private static final String TERRAFORM_BACKEND_CONFIGS_FILE_NAME = "backend_configs";
  private static final String REMOTE_STATE_FILE_PATH = ".terraform/terraform.tfstate";
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

  private Pattern varList = Pattern.compile("^\\s*\\[.*?]\\s*$");

  private void saveVariable(BufferedWriter writer, String key, String value) throws IOException {
    // If the variable is wrapped with [] square brackets, we assume it is a list and we keep it as is.
    if (varList.matcher(value).matches()) {
      writer.write(format("%s = %s%n", key, value));
      return;
    }

    writer.write(format("%s = \"%s\"%n", key, value.replaceAll("\"", "\\\"")));
  }

  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    GitConfig gitConfig = parameters.getSourceRepo();
    saveExecutionLog(parameters, "Branch: " + gitConfig.getBranch() + "\nPath: " + parameters.getScriptPath(),
        CommandExecutionStatus.RUNNING);
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    if (isNotEmpty(gitConfig.getReference())) {
      saveExecutionLog(parameters, format("Inheriting git state at commit id: [%s]", gitConfig.getReference()),
          CommandExecutionStatus.RUNNING);
    }

    try {
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails());
      gitClient.ensureRepoLocallyClonedAndUpdated(gitConfig);
    } catch (RuntimeException ex) {
      logger.error("Exception in processing git operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
    String scriptDirectory = resolveScriptDirectory(gitConfig, parameters.getScriptPath());
    logger.info("Script Directory: " + scriptDirectory);

    String sourceRepoReference = getLatestCommitSHAFromLocalRepo(gitConfig);

    File tfVariablesFile = Paths.get(scriptDirectory, TERRAFORM_VARIABLES_FILE_NAME).toFile();
    File tfBackendConfigsFile = Paths.get(scriptDirectory, TERRAFORM_BACKEND_CONFIGS_FILE_NAME).toFile();

    ensureLocalCleanup(scriptDirectory);

    boolean usingRemoteState = isRemoteStateConfigured(scriptDirectory);

    try {
      if (!usingRemoteState) {
        File tfStateFile = Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile();

        if (parameters.getCurrentStateFileId() != null) {
          try (InputStream stateRemoteInputStream = delegateFileManager.downloadByFileId(
                   FileBucket.TERRAFORM_STATE, parameters.getCurrentStateFileId(), parameters.getAccountId())) {
            FileUtils.copyInputStreamToFile(stateRemoteInputStream, tfStateFile);
          }
        } else {
          FileUtils.deleteQuietly(tfStateFile);
        }
      }

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

      File tfOutputsFile = Paths.get(scriptDirectory, TERRAFORM_VARIABLES_FILE_NAME).toFile();
      String targetArgs = getTargetArgs(parameters.getTargets());

      int code;
      ActivityLogOutputStream activityLogOutputStream = new ActivityLogOutputStream(parameters);
      PlanLogOutputStream planLogOutputStream = new PlanLogOutputStream(parameters, new ArrayList<>());
      switch (parameters.getCommand()) {
        case APPLY: {
          /**
           * The "echo yes" in the terraform init command below is required as a workaround for a Tf bug.
           * In some versions of Tf, we have seen that -force-copy flag was not honoured by Tf.
           * Please do not remove.
           */
          code = executeShellCommand(
              format("echo \"yes\" | terraform init -force-copy %s && echo \"Terraform init... done\"",
                  tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                                : ""),
              scriptDirectory, parameters, activityLogOutputStream);
          if (code == 0) {
            code = executeShellCommand(
                format("terraform refresh -input=false %s && echo \"Terraform refresh... done\"", targetArgs),
                scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0) {
            code = executeShellCommand(
                format("terraform plan -out=tfplan -input=false %s && echo \"Terraform plan ... done\"", targetArgs),
                scriptDirectory, parameters, planLogOutputStream);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            code = executeShellCommand("terraform apply -input=false tfplan && echo \"Terraform apply... done\"",
                scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0 && !parameters.isRunPlanOnly()) {
            code = executeShellCommand(format("terraform output --json > %s", tfOutputsFile.toString()),
                scriptDirectory, parameters, activityLogOutputStream);
          }
          break;
        }
        case DESTROY: {
          code = executeShellCommand(
              format("terraform init -input=false %s && echo \"Terraform init... done\"",
                  tfBackendConfigsFile.exists() ? format("-backend-config=%s", tfBackendConfigsFile.getAbsolutePath())
                                                : ""),
              scriptDirectory, parameters, activityLogOutputStream);
          if (code == 0) {
            code = executeShellCommand(
                format("terraform refresh -input=false %s && echo \"Terraform refresh... done\"", targetArgs),
                scriptDirectory, parameters, activityLogOutputStream);
          }
          if (code == 0) {
            code = executeShellCommand(
                format("terraform destroy -force %s && echo \"Terraform destroy... done\"", targetArgs),
                scriptDirectory, parameters, activityLogOutputStream);
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

      File tfStateFile = getTerraformStateFile(scriptDirectory);
      if (tfStateFile != null) {
        try (InputStream initialStream = new FileInputStream(tfStateFile)) {
          delegateFileManager.upload(delegateFile, initialStream);
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
              .executionStatus(code == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
              .errorMessage(code == 0 ? null : "The terraform command exited with code " + code);

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

  private void ensureLocalCleanup(String scriptDirectory) {
    FileUtils.deleteQuietly(Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile());
    FileUtils.deleteQuietly(Paths.get(scriptDirectory, REMOTE_STATE_FILE_PATH).toFile());
  }

  @VisibleForTesting
  public String getTargetArgs(List<String> targets) {
    StringBuffer targetArgs = new StringBuffer();
    if (isNotEmpty(targets)) {
      for (String target : targets) {
        targetArgs.append("-target=" + target + " ");
      }
    }
    return targetArgs.toString();
  }

  private boolean isRemoteStateConfigured(String scriptDirectory) {
    HCLParser hclParser = new HCLParser();

    File[] allFiles = new File(scriptDirectory).listFiles();
    if (isNotEmpty(allFiles)) {
      for (File file : allFiles) {
        if (file.getName().endsWith(TERRAFORM_SCRIPT_FILE_EXTENSION)) {
          Map<String, Object> parsedContents;
          try {
            byte[] tfContent = Files.readAllBytes(Paths.get(scriptDirectory, file.getName()));

            if (hclParser == null) {
              throw new WingsException(ErrorCode.UNKNOWN_ERROR, "Unable to instantiate HCL Parser");
            }
            parsedContents = hclParser.parse(new String(tfContent, Charsets.UTF_8));
          } catch (IOException | HCLParserException e) {
            logger.error("HCL Parser Exception for file [" + file.getAbsolutePath() + "], ", e);
            throw new WingsException(
                ErrorCode.GENERAL_ERROR, "Invalid Terraform File [" + file.getAbsolutePath() + "] : " + e.getMessage());
          }

          LinkedHashMap<String, Object> terraform = (LinkedHashMap) parsedContents.get("terraform");
          if (isNotEmpty(terraform)) {
            if (terraform.getOrDefault("backend", null) != null) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private File getTerraformStateFile(String scriptDirectory) {
    File tfStateFile = Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile();

    if (tfStateFile.exists()) {
      return tfStateFile;
    }

    File tfRemoteStateDirectory = Paths.get(scriptDirectory, ".terraform").toFile();
    if (tfRemoteStateDirectory.exists()) {
      File tfRemoteStateFile = Paths.get(tfRemoteStateDirectory.getAbsolutePath(), TERRAFORM_STATE_FILE_NAME).toFile();
      if (tfRemoteStateFile.exists()) {
        return tfRemoteStateFile;
      }
    }

    return null;
  }

  private String getLatestCommitSHAFromLocalRepo(GitConfig gitConfig) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitConfig));
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

  private String resolveScriptDirectory(GitConfig gitConfig, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty("user.dir")).toString(), gitClientHelper.getRepoDirectory(gitConfig),
            scriptPath == null ? "" : scriptPath)
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
