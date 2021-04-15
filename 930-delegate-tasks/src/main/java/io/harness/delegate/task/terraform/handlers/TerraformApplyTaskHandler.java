package io.harness.delegate.task.terraform.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.TerraformConstants.RESOURCE_READY_WAIT_TIME_SECONDS;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_SCRIPT_DIR;
import static io.harness.provision.TerraformConstants.TF_VAR_FILES_DIR;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.shell.SshSessionConfigMapper;
import io.harness.delegate.task.terraform.TerraformBaseHelper;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.git.GitClientHelper;
import io.harness.git.GitClientV2;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;
import io.harness.terraform.TerraformHelperUtils;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;

@Slf4j
@OwnedBy(CDP)
public class TerraformApplyTaskHandler extends TerraformAbstractTaskHandler {
  @Inject TerraformBaseHelper terraformBaseHelper;
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject GitClientV2 gitClient;
  @Inject GitClientHelper gitClientHelper;
  @Inject DelegateFileManagerBase delegateFileManager;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;
  @Inject private NGGitService ngGitService;

  @Override
  public TerraformTaskNGResponse executeTaskInternal(
      TerraformTaskNGParameters taskParameters, String delegateId, String taskId, LogCallback logCallback) {
    GitStoreDelegateConfig confileFileGitStore = taskParameters.getConfigFile().getGitStoreDelegateConfig();
    GitConfigDTO configFileGitConfigDTO =
        (GitConfigDTO) taskParameters.getConfigFile().getGitStoreDelegateConfig().getGitConfigDTO();

    if (isNotEmpty(confileFileGitStore.getBranch())) {
      logCallback.saveExecutionLog("Branch: " + confileFileGitStore.getBranch(), INFO, CommandExecutionStatus.RUNNING);
    }

    logCallback.saveExecutionLog(
        "Normalized Path: " + confileFileGitStore.getPaths().get(0), INFO, CommandExecutionStatus.RUNNING);

    if (isNotEmpty(confileFileGitStore.getCommitId())) {
      logCallback.saveExecutionLog(
          format("%nInheriting git state at commit id: [%s]", confileFileGitStore.getCommitId()), INFO,
          CommandExecutionStatus.RUNNING);
    }

    GitBaseRequest gitBaseRequestForConfigFile;
    try {
      secretDecryptionService.decrypt(
          configFileGitConfigDTO.getGitAuth(), confileFileGitStore.getEncryptedDataDetails());

      gitBaseRequestForConfigFile =
          getGitBaseRequestForConfigFile(taskParameters.getAccountId(), confileFileGitStore, configFileGitConfigDTO);

      gitClient.ensureRepoLocallyClonedAndUpdated(gitBaseRequestForConfigFile);
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
      TerraformHelperUtils.copyFilesToWorkingDirectory(
          gitClientHelper.getRepoDirectory(gitBaseRequestForConfigFile), workingDir);
    } catch (Exception ex) {
      log.error("Exception in copying files to provisioner specific directory", ex);
      FileUtils.deleteQuietly(new File(baseDir));
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }

    String scriptDirectory =
        terraformBaseHelper.resolveScriptDirectory(workingDir, confileFileGitStore.getPaths().get(0));
    log.info("Script Directory: " + scriptDirectory);
    logCallback.saveExecutionLog(
        format("Script Directory: [%s]", scriptDirectory), INFO, CommandExecutionStatus.RUNNING);

    try {
      TerraformHelperUtils.ensureLocalCleanup(scriptDirectory);
      terraformBaseHelper.downloadTfStateFile(taskParameters.getWorkspace(), taskParameters.getAccountId(),
          taskParameters.getCurrentStateFileId(), scriptDirectory);
    } catch (IOException ioException) {
      log.warn("Exception Occurred when cleaning Terraform local directory", ioException);
    }

    File tfOutputsFile =
        Paths.get(scriptDirectory, format(TERRAFORM_VARIABLES_FILE_NAME, taskParameters.getProvisionerIdentifier()))
            .toFile();

    try (PlanJsonLogOutputStream planJsonLogOutputStream = new PlanJsonLogOutputStream()) {
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
              .planJsonLogOutputStream(planJsonLogOutputStream)
              .build();

      CliResponse response = terraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);

      logCallback.saveExecutionLog(
          format("Waiting: [%s] seconds for resources to be ready", RESOURCE_READY_WAIT_TIME_SECONDS), INFO,
          CommandExecutionStatus.RUNNING);
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
          .commitIdForConfigFilesMap(buildcommitIdToFetchedFilesMap(taskParameters.getAccountId(),
              taskParameters.getConfigFile().getIdentifier(), gitBaseRequestForConfigFile,
              taskParameters.getRemoteVarfiles()))
          .encryptedTfPlan(taskParameters.getEncryptedTfPlan())
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .build();

    } catch (TerraformCommandExecutionException terraformCommandExecutionException) {
      log.warn("Failed to execute TerraformApplyStep", terraformCommandExecutionException);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(terraformCommandExecutionException))
          .build();
    } catch (Exception exception) {
      log.warn("Exception Occurred", exception);
      return TerraformTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(exception))
          .build();
    }
  }

  private GitBaseRequest getGitBaseRequestForConfigFile(
      String accountId, GitStoreDelegateConfig confileFileGitStore, GitConfigDTO configFileGitConfigDTO) {
    SshSessionConfig sshSessionConfig = null;
    if (configFileGitConfigDTO.getGitAuthType() == SSH) {
      if (confileFileGitStore.getSshKeySpecDTO() == null) {
        throw new InvalidRequestException(
            format("SSHKeySpecDTO is null for connector %s", confileFileGitStore.getConnectorName()));
      }
      sshSessionConfig = sshSessionConfigMapper.getSSHSessionConfig(
          confileFileGitStore.getSshKeySpecDTO(), confileFileGitStore.getEncryptedDataDetails());
    }

    return GitBaseRequest.builder()
        .branch(confileFileGitStore.getBranch())
        .commitId(confileFileGitStore.getCommitId())
        .repoUrl(configFileGitConfigDTO.getUrl())
        .authRequest(
            ngGitService.getAuthRequest((GitConfigDTO) confileFileGitStore.getGitConfigDTO(), sshSessionConfig))
        .accountId(accountId)
        .connectorId(confileFileGitStore.getConnectorName())
        .build();
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

  private Map<String, String> buildcommitIdToFetchedFilesMap(String accountId, String configFileIdentifier,
      GitBaseRequest gitBaseRequestForConfigFile, List<GitFetchFilesConfig> varFilesgitFetchFilesConfigList) {
    Map<String, String> commitIdForConfigFilesMap = new HashMap<>();
    // Config File
    commitIdForConfigFilesMap.put(configFileIdentifier, getLatestCommitSHAFromLocalRepo(gitBaseRequestForConfigFile));
    // Add remote var files
    if (isNotEmpty(varFilesgitFetchFilesConfigList)) {
      addVarFilescommitIdstoMap(accountId, varFilesgitFetchFilesConfigList, commitIdForConfigFilesMap);
    }
    return commitIdForConfigFilesMap;
  }

  public void addVarFilescommitIdstoMap(String accountId, List<GitFetchFilesConfig> varFilesgitFetchFilesConfigList,
      Map<String, String> commitIdForConfigFilesMap) {
    for (GitFetchFilesConfig config : varFilesgitFetchFilesConfigList) {
      GitStoreDelegateConfig gitStoreDelegateConfig = config.getGitStoreDelegateConfig();
      GitConfigDTO gitConfigDTO = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();

      SshSessionConfig sshSessionConfig = sshSessionConfigMapper.getSSHSessionConfig(
          gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());

      GitBaseRequest gitBaseRequest =
          GitBaseRequest.builder()
              .branch(gitStoreDelegateConfig.getBranch())
              .commitId(gitStoreDelegateConfig.getCommitId())
              .repoUrl(gitConfigDTO.getUrl())
              .connectorId(gitStoreDelegateConfig.getConnectorName())
              .authRequest(ngGitService.getAuthRequest(
                  (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO(), sshSessionConfig))
              .accountId(accountId)
              .build();
      commitIdForConfigFilesMap.putIfAbsent(config.getIdentifier(), getLatestCommitSHAFromLocalRepo(gitBaseRequest));
    }
  }

  public String getLatestCommitSHAFromLocalRepo(GitBaseRequest gitBaseRequest) {
    return terraformBaseHelper.getLatestCommitSHA(new File(gitClientHelper.getRepoDirectory(gitBaseRequest)));
  }
}
