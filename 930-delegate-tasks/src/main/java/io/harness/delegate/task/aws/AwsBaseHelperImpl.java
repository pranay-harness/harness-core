package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.AWSConstants.AWS_SAM_APP_REPOSITORY_DIR;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.shell.SshSessionConfigMapper;
import io.harness.exception.AwsSamCommandExecutionException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.git.GitClientHelper;
import io.harness.git.GitClientV2;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.secretmanagerclient.EncryptDecryptHelper;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@OwnedBy(CDP)
public class AwsBaseHelperImpl implements AwsBaseHelper {
  @Inject DelegateFileManagerBase delegateFileManagerBase;
  @Inject EncryptDecryptHelper encryptDecryptHelper;
  @Inject GitClientV2 gitClient;
  @Inject GitClientHelper gitClientHelper;
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject SshSessionConfigMapper sshSessionConfigMapper;
  @Inject NGGitService ngGitService;
  @Inject DelegateFileManagerBase delegateFileManager;

  public GitBaseRequest getGitBaseRequestForConfigFile(
      String accountId, GitStoreDelegateConfig confileFileGitStore, GitConfigDTO configFileGitConfigDTO) {
    secretDecryptionService.decrypt(configFileGitConfigDTO.getGitAuth(), confileFileGitStore.getEncryptedDataDetails());

    SshSessionConfig sshSessionConfig = null;
    if (configFileGitConfigDTO.getGitAuthType() == SSH) {
      sshSessionConfig = getSshSessionConfig(confileFileGitStore);
    }

    return GitBaseRequest.builder()
        .branch(confileFileGitStore.getBranch())
        .commitId(confileFileGitStore.getCommitId())
        .repoUrl(configFileGitConfigDTO.getUrl())
        .repoType(GitRepositoryType.AWS_SAM)
        .authRequest(
            ngGitService.getAuthRequest((GitConfigDTO) confileFileGitStore.getGitConfigDTO(), sshSessionConfig))
        .accountId(accountId)
        .connectorId(confileFileGitStore.getConnectorName())
        .build();
  }

  public String fetchAwsSamAppDirectory(GitBaseRequest gitBaseRequestForConfigFile, String accountId, String workspace,
      String currentStateFileId, GitStoreDelegateConfig confileFileGitStore, LogCallback logCallback, String scriptPath,
      String baseDir) {
    fetchConfigFileAndCloneLocally(gitBaseRequestForConfigFile, logCallback);

    String workingDir = Paths.get(baseDir, AWS_SAM_APP_REPOSITORY_DIR).toString();
    try {
      ensureLocalCleanup(workingDir);
    } catch (IOException ioException) {
      log.warn("Exception Occurred when cleaning AWS_SAM local directory", ioException);
    }

    copyConfigFilestoWorkingDirectory(logCallback, gitBaseRequestForConfigFile, baseDir, workingDir);

    log.info("working Directory: " + workingDir);
    logCallback.saveExecutionLog(format("Script Directory: [%s]", workingDir), INFO, CommandExecutionStatus.RUNNING);

    return workingDir;
  }

  public void copyFilesToWorkingDirectory(String sourceDir, String destinationDir) throws IOException {
    File dest = new File(destinationDir);
    File src = new File(sourceDir);
    FileUtils.deleteDirectory(dest);
    FileUtils.copyDirectory(src, dest);
    FileIo.waitForDirectoryToBeAccessibleOutOfProcess(dest.getPath(), 10);
  }

  public void ensureLocalCleanup(String scriptDirectory) throws IOException {
    FileUtils.deleteQuietly(Paths.get(scriptDirectory).toFile());
    try {
      deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory).toString());
    } catch (IOException e) {
      log.warn("Failed to delete .aws-sam folder");
    }
  }

  public void copyConfigFilestoWorkingDirectory(
      LogCallback logCallback, GitBaseRequest gitBaseRequestForConfigFile, String baseDir, String workingDir) {
    try {
      copyFilesToWorkingDirectory(gitClientHelper.getRepoDirectory(gitBaseRequestForConfigFile), workingDir);
    } catch (Exception ex) {
      log.error("Exception in copying files to provisioner specific directory", ex);
      FileUtils.deleteQuietly(new File(baseDir));
      logCallback.saveExecutionLog(
          "Failed copying files to provisioner specific directory", ERROR, CommandExecutionStatus.FAILURE);
    }
  }

  public void fetchConfigFileAndCloneLocally(GitBaseRequest gitBaseRequestForConfigFile, LogCallback logCallback) {
    try {
      gitClient.ensureRepoLocallyClonedAndUpdated(gitBaseRequestForConfigFile);
    } catch (RuntimeException ex) {
      logCallback.saveExecutionLog(format("Failed performing git operation. Reason: %s", ex.getMessage()), ERROR,
          CommandExecutionStatus.RUNNING);
      throw new AwsSamCommandExecutionException(format("Exception in processing git operation"), WingsException.USER);
    }
  }

  private SshSessionConfig getSshSessionConfig(GitStoreDelegateConfig gitStoreDelegateConfig) {
    if (gitStoreDelegateConfig.getSshKeySpecDTO() == null) {
      throw new InvalidRequestException(
          format("SSHKeySpecDTO is null for connector %s", gitStoreDelegateConfig.getConnectorName()));
    }
    return sshSessionConfigMapper.getSSHSessionConfig(
        gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
  }
}
