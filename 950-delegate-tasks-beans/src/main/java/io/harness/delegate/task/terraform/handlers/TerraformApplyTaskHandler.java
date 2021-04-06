package io.harness.delegate.task.terraform.handlers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.TerraformConstants.RESOURCE_READY_WAIT_TIME_SECONDS;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_SCRIPT_DIR;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_DIR;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.cli.CliResponse;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGLogCallback;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformCommandUnit;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.git.GitClientHelper;
import io.harness.git.GitClientV2;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.terraform.TerraformHelperUtils;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

@Slf4j
public class TerraformApplyTaskHandler extends TerraformAbstractTaskHandler {
  @Inject TerraformBaseHelper terraformBaseHelper;
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject GitClientV2 gitClient;
  @Inject GitClientHelper gitClientHelper;
  @Inject DelegateFileManagerBase delegateFileManager;

  @Override
  public TerraformTaskNGResponse executeTaskInternal(TerraformTaskNGParameters taskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress,
      AuthRequest authRequest, String delegateId, String taskId) {
    LogCallback logCallback =
        getLogCallback(logStreamingTaskClient, TerraformCommandUnit.Apply.name(), true, commandUnitsProgress);

    GitStoreDelegateConfig gitStoreDelegateConfig = taskParameters.getConfigFile().getGitStoreDelegateConfig();
    GitConfigDTO gitConfigDTO =
        (GitConfigDTO) taskParameters.getConfigFile().getGitStoreDelegateConfig().getGitConfigDTO();

    if (isNotEmpty(gitStoreDelegateConfig.getBranch())) {
      logCallback.saveExecutionLog(
          "Branch: " + gitStoreDelegateConfig.getBranch(), INFO, CommandExecutionStatus.RUNNING);
    }

    // TODO: Why are there multiple paths?
    logCallback.saveExecutionLog(
        "Normalized Path: " + taskParameters.getConfigFile().getGitStoreDelegateConfig().getPaths(), INFO,
        CommandExecutionStatus.RUNNING);
    /*
    gitConfig.setGitRepoType(GitRepositoryType.TERRAFORM);

    if (isNotEmpty(gitConfig.getReference())) {
      saveExecutionLog(format("%nInheriting git state at commit id: [%s]", gitConfig.getReference()),
          CommandExecutionStatus.RUNNING, INFO, logCallback);
    }
     */
    GitBaseRequest gitBaseRequest = GitBaseRequest.builder()
                                        .branch(gitStoreDelegateConfig.getBranch())
                                        .commitId(gitStoreDelegateConfig.getCommitId())
                                        .repoUrl(gitConfigDTO.getUrl())
                                        .authRequest(authRequest)
                                        .accountId(taskParameters.getAccountId())
                                        .connectorId(gitStoreDelegateConfig.getConnectorName())
                                        .build();

    EncryptedRecordData encryptedTfPlan = taskParameters.getEncryptedTfPlan();

    try {
      secretDecryptionService.decrypt(gitConfigDTO.getGitAuth(),
          taskParameters.getConfigFile().getGitStoreDelegateConfig().getEncryptedDataDetails());

      gitClient.ensureRepoLocallyClonedAndUpdated(gitBaseRequest);
    } catch (RuntimeException ex) {
      log.error("Exception in processing git operation", ex);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(TerraformHelperUtils.getGitExceptionMessageIfExists(ex))
          .build();
    }

    String baseDir =
        terraformBaseHelper.resolveBaseDir(taskParameters.getAccountId(), taskParameters.getProvisionerIdentifier());
    String tfVarDirectory = Paths.get(baseDir, TF_VAR_FILES_DIR).toString();
    String workingDir = Paths.get(baseDir, TF_SCRIPT_DIR).toString();

    if (isNotEmpty(taskParameters.getRemoteVarfiles())) {
      fetchRemoteTfVarFiles(taskParameters, logCallback, tfVarDirectory);
    }

    try {
      TerraformHelperUtils.copyFilesToWorkingDirectory(gitClientHelper.getRepoDirectory(gitBaseRequest), workingDir);
    } catch (Exception ex) {
      log.error("Exception in copying files to provisioner specific directory", ex);
      FileUtils.deleteQuietly(new File(baseDir));
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }

    // TODO: Verify Second Parameter
    String scriptDirectory =
        terraformBaseHelper.resolveScriptDirectory(workingDir, taskParameters.getConfigFile().getIdentifier());

    log.info("Script Directory: " + scriptDirectory);
    logCallback.saveExecutionLog(
        format("Script Directory: [%s]", scriptDirectory), INFO, CommandExecutionStatus.RUNNING);

    try {
      TerraformHelperUtils.ensureLocalCleanup(scriptDirectory);

      String sourceRepoReference = gitStoreDelegateConfig.getCommitId() != null
          ? gitStoreDelegateConfig.getCommitId()
          : getLatestCommitSHAFromLocalRepo(gitBaseRequest);

      terraformBaseHelper.downloadTfStateFile(taskParameters.getWorkspace(), taskParameters.getAccountId(),
          taskParameters.getCurrentStateFileId(), scriptDirectory);
    } catch (IOException ioException) {
      log.warn("Exception Occurred when cleaning Terraform local directory", ioException);
    }

    File tfOutputsFile =
        Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, taskParameters.getProvisionerIdentifier()))
            .toFile();

    try {
      TerraformExecuteStepRequest terraformExecuteStepRequest =
          TerraformExecuteStepRequest.builder()
              .tfBackendConfigsFile(taskParameters.getBackendConfig())
              .tfOutputsFile(tfOutputsFile.getAbsolutePath())
              .tfVarFilePaths(taskParameters.getInlineVarFiles())
              .workspace(taskParameters.getWorkspace())
              .targets(taskParameters.getTargets())
              .scriptDirectory(scriptDirectory)
              .encryptedTfPlan(taskParameters.getEncryptedTfPlan())
              .encryptionConfig(taskParameters.getEncryptionConfig())
              .envVars(taskParameters.getEnvironmentVariables())
              .isSaveTerraformJson(taskParameters.isSaveTerraformStateJson())
              .logCallback(logCallback)
              .build();

      CliResponse response = terraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);

      logCallback.saveExecutionLog(
          format("Waiting: [%s] seconds for resources to be ready", String.valueOf(RESOURCE_READY_WAIT_TIME_SECONDS)),
          INFO, CommandExecutionStatus.RUNNING);
      sleep(ofSeconds(RESOURCE_READY_WAIT_TIME_SECONDS));

      logCallback.saveExecutionLog("Script execution finished with status: " + response.getCommandExecutionStatus(),
          INFO, response.getCommandExecutionStatus());

      final DelegateFile delegateFile = aDelegateFile()
                                            .withAccountId(taskParameters.getAccountId())
                                            .withDelegateId(delegateId)
                                            .withTaskId(taskId)
                                            .withEntityId(taskParameters.getProvisionerIdentifier())
                                            .withBucket(FileBucket.TERRAFORM_STATE)
                                            .withFileName(TERRAFORM_STATE_FILE_NAME)
                                            .build();

      File tfStateFile = TerraformHelperUtils.getTerraformStateFile(scriptDirectory, taskParameters.getWorkspace());
      if (tfStateFile != null) {
        try (InputStream initialStream = new FileInputStream(tfStateFile)) {
          delegateFileManager.upload(delegateFile, initialStream);
        }
      } else {
        try (InputStream nullInputStream = new NullInputStream(0)) {
          delegateFileManager.upload(delegateFile, nullInputStream);
        }
      }

      return TerraformTaskNGResponse.builder()
          .outputs(new String(Files.readAllBytes(tfOutputsFile.toPath()), Charsets.UTF_8))
          .build();

    } catch (TerraformCommandExecutionException terraformCommandExecutionException) {
      log.warn("Failed to execute TerraformApplyStep", terraformCommandExecutionException);
    } catch (Exception e) {
      log.warn("Exception Occurred", e);
    }

    return null;
  }

  private void fetchRemoteTfVarFiles(
      TerraformTaskNGParameters taskParameters, LogCallback logCallback, String tfVarDirectory) {
    for (GitFetchFilesConfig gitFetchFilesConfig : taskParameters.getRemoteVarfiles()) {
      GitConfigDTO configDTO = (GitConfigDTO) gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO();
      logCallback.saveExecutionLog(format("Fetching TfVar files from Git repository: [%s]", configDTO.getUrl()), INFO,
          CommandExecutionStatus.RUNNING);
      secretDecryptionService.decrypt(
          configDTO.getGitAuth(), taskParameters.getConfigFile().getGitStoreDelegateConfig().getEncryptedDataDetails());
      gitClient.downloadFiles(DownloadFilesRequest.builder()
                                  .branch(gitFetchFilesConfig.getGitStoreDelegateConfig().getBranch())
                                  .commitId(gitFetchFilesConfig.getGitStoreDelegateConfig().getCommitId())
                                  .filePaths(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths())
                                  .connectorId(gitFetchFilesConfig.getGitStoreDelegateConfig().getConnectorName())
                                  .recursive(true)
                                  .destinationDirectory(tfVarDirectory)
                                  .build());

      logCallback.saveExecutionLog(
          format("TfVar Git directory: [%s]", tfVarDirectory), INFO, CommandExecutionStatus.RUNNING);
    }
  }

  private LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }

  public String getLatestCommitSHAFromLocalRepo(GitBaseRequest gitBaseRequest) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitBaseRequest));
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
}
