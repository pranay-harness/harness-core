package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformExecutionData.TerraformExecutionDataBuilder;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class TerraformProvisionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TerraformProvisionTask.class);
  private static final String TERRAFORM_STATE_FILE_NAME = "terraform.tfstate";
  private static final String TERRAFORM_VARIABLES_FILE_NAME = "terraform.tfvars";
  private static final String TERRAFORM_OUTPUTS_FILE_NAME = "terraform.tfouts";

  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

  public TerraformProvisionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TerraformExecutionData run(Object[] parameters) {
    return run((TerraformProvisionParameters) parameters[0]);
  }

  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "DM_DEFAULT_ENCODING", "REC_CATCH_EXCEPTION"})
  private TerraformExecutionData run(TerraformProvisionParameters parameters) {
    GitConfig gitConfig = parameters.getSourceRepo();
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    try {
      encryptionService.decrypt(gitConfig, parameters.getSourceRepoEncryptionDetails());
      gitClient.ensureRepoLocallyClonedAndUpdated(gitConfig);
    } catch (RuntimeException ex) {
      logger.error("Exception in processing git operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }

    try {
      String scriptDirectory = resolveScriptDirectory(gitConfig, parameters.getScriptPath());
      File tfStateFile = Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile();

      if (parameters.getCurrentStateFileId() != null) {
        try (InputStream stateRemoteInputStream = delegateFileManager.downloadByFileId(
                 FileBucket.TERRAFORM_STATE, parameters.getCurrentStateFileId(), parameters.getAccountId(), false)) {
          FileUtils.copyInputStreamToFile(stateRemoteInputStream, tfStateFile);
        }
      } else {
        FileUtils.deleteQuietly(tfStateFile);
      }

      File tfVariablesFile = Paths.get(scriptDirectory, TERRAFORM_VARIABLES_FILE_NAME).toFile();
      if (!isEmpty(parameters.getVariables())) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tfVariablesFile))) {
          for (Entry<String, String> entry : parameters.getVariables().entrySet()) {
            // TODO: we should probably do some escaping here
            writer.write(String.format("%s = \"%s\"%n", entry.getKey(), entry.getValue()));
          }

          for (Entry<String, EncryptedDataDetail> entry : parameters.getEncryptedVariables().entrySet()) {
            String value = new String(encryptionService.getDecryptedValue(entry.getValue()));

            // TODO: we should probably do some escaping here
            writer.write(String.format("%s = \"%s\"%n", entry.getKey(), value));
          }
        }
      } else {
        FileUtils.deleteQuietly(tfVariablesFile);
      }

      File tfOutputsFile = Paths.get(scriptDirectory, TERRAFORM_VARIABLES_FILE_NAME).toFile();

      String joinedCommands = null;
      if ("Apply".equals(parameters.getCommandUnitName())) {
        joinedCommands = Joiner.on(" && ").join(asList("cd " + scriptDirectory, "terraform init -input=false",
            "terraform refresh -input=false", "terraform plan -out=tfplan -input=false",
            "terraform apply -input=false tfplan", "(terraform output --json > " + tfOutputsFile.toString() + ")"));
      } else if ("Destroy".equals(parameters.getCommandUnitName())) {
        joinedCommands = Joiner.on(" && ").join(asList("cd " + scriptDirectory, "terraform init -input=false",
            "terraform refresh -input=false", "terraform destroy -force"));
      }

      List<Boolean> instances = asList(false);
      Pattern detectChange =
          Pattern.compile("Apply complete! Resources: [1-9][0-9]* added, [0-9]+ changed, [0-9]+ destroyed.");

      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(parameters.getTimeoutInMillis(), TimeUnit.MILLISECONDS)
                                            .command("/bin/sh", "-c", joinedCommands)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                if (detectChange.matcher(line).find()) {
                                                  instances.set(0, true);
                                                }
                                                saveExecutionLog(parameters, line, CommandExecutionStatus.RUNNING);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      int code = processResult.getExitValue();

      if (code == 0 && instances.get(0)) {
        // This might seem strange, but we would like to give the cloud an extra time to initialize the
        // instances, while letting the customers to believe that we are doing something smart about it, we just sleep.
        int i = 0;
        while (i < 15) {
          saveExecutionLog(parameters, "Test instances for readiness...", CommandExecutionStatus.RUNNING);
          final int s = new Random().nextInt(5);
          sleep(ofSeconds(s));
          i += s;
        }
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

      try (InputStream initialStream = new FileInputStream(tfStateFile)) {
        delegateFileManager.upload(delegateFile, initialStream);
      }

      final TerraformExecutionDataBuilder terraformExecutionDataBuilder =
          TerraformExecutionData.builder()
              .entityId(delegateFile.getEntityId())
              .stateFileId(delegateFile.getFileId())
              .executionStatus(code == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
              .errorMessage(code == 0 ? null : "The terraform command exited with code " + code);

      if (!"Destroy".equals(parameters.getCommandUnitName())) {
        terraformExecutionDataBuilder.outputs(new String(Files.readAllBytes(tfOutputsFile.toPath())));
      }

      return terraformExecutionDataBuilder.build();

    } catch (Exception ex) {
      logger.error("Exception in processing terraform operation", ex);
      return TerraformExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }

  private String resolveScriptDirectory(GitConfig gitConfig, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty("user.dir")).toString(), gitClient.getRepoDirectory(gitConfig),
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
            .withCommandUnitName(parameters.getCommandUnitName())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }
}
