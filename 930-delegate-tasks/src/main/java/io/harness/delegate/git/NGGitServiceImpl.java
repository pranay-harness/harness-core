package io.harness.delegate.git;

import static io.harness.git.model.GitRepositoryType.YAML;
import static io.harness.shell.SshSessionFactory.getSSHSession;
import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.task.shell.SshSessionConfigMapper;
import io.harness.exception.InvalidRequestException;
import io.harness.git.GitClientV2;
import io.harness.git.UsernamePasswordAuthRequest;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.git.model.JgitSshAuthRequest;
import io.harness.shell.SshSessionConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import javax.validation.executable.ValidateOnExecution;

import io.harness.task.git.NGGitService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class NGGitServiceImpl implements NGGitService {
  @Inject private GitClientV2 gitClientV2;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;

  @Override
  public void validate(GitConfigDTO gitConfig, String accountId, SshSessionConfig sshSessionConfig) {
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    setGitBaseRequest(gitConfig, accountId, gitBaseRequest, YAML, sshSessionConfig);
    gitClientV2.validate(gitBaseRequest);
  }
  @Override
  public void validateOrThrow(GitConfigDTO gitConfig, String accountId, SshSessionConfig sshSessionConfig) {
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    setGitBaseRequest(gitConfig, accountId, gitBaseRequest, YAML, sshSessionConfig);
    gitClientV2.validateOrThrow(gitBaseRequest);
  }

  @VisibleForTesting
  void setGitBaseRequest(GitConfigDTO gitConfig, String accountId, GitBaseRequest gitBaseRequest,
      GitRepositoryType repositoryType, SshSessionConfig sshSessionConfig) {
    gitBaseRequest.setAuthRequest(getAuthRequest(gitConfig, sshSessionConfig));
    gitBaseRequest.setBranch(gitConfig.getBranchName());
    gitBaseRequest.setRepoType(repositoryType);
    gitBaseRequest.setRepoUrl(gitConfig.getUrl());
    gitBaseRequest.setAccountId(accountId);
  }

  public AuthRequest getAuthRequest(GitConfigDTO gitConfig, SshSessionConfig sshSessionConfig) {
    switch (gitConfig.getGitAuthType()) {
      case SSH:
        return JgitSshAuthRequest.builder().factory(getSshSessionFactory(sshSessionConfig)).build();
      case HTTP:
        GitHTTPAuthenticationDTO httpAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        String userName = getSecretAsStringFromPlainTextOrSecretRef(
            httpAuthenticationDTO.getUsername(), httpAuthenticationDTO.getUsernameRef());
        return UsernamePasswordAuthRequest.builder()
            .username(userName)
            .password(httpAuthenticationDTO.getPasswordRef().getDecryptedValue())
            .build();
      default:
        throw new InvalidRequestException("Unknown auth type.");
    }
  }

  private SshSessionFactory getSshSessionFactory(SshSessionConfig sshSessionConfig) {
    return new JschConfigSessionFactory() {
      @Override
      protected Session createSession(OpenSshConfig.Host hc, String user, String host, int port, FS fs)
          throws JSchException {
        sshSessionConfig.setPort(port); // use port from repo URL
        sshSessionConfig.setHost(host);
        return getSSHSession(sshSessionConfig);
      }

      @Override
      protected void configure(OpenSshConfig.Host hc, Session session) {}
    };
  }

  @Override
  public CommitAndPushResult commitAndPush(GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest,
      String accountId, SshSessionConfig sshSessionConfig) {
    setGitBaseRequest(gitConfig, accountId, commitAndPushRequest, YAML, sshSessionConfig);
    return gitClientV2.commitAndPush(commitAndPushRequest);
  }

  @Override
  public FetchFilesResult fetchFilesByPath(GitStoreDelegateConfig gitStoreDelegateConfig, String accountId,
      SshSessionConfig sshSessionConfig, GitConfigDTO gitConfigDTO) {
    FetchFilesByPathRequest fetchFilesByPathRequest = FetchFilesByPathRequest.builder()
                                                          .authRequest(getAuthRequest(gitConfigDTO, sshSessionConfig))
                                                          .filePaths(gitStoreDelegateConfig.getPaths())
                                                          .recursive(true)
                                                          .accountId(accountId)
                                                          .branch(gitStoreDelegateConfig.getBranch())
                                                          .commitId(gitStoreDelegateConfig.getCommitId())
                                                          .connectorId(gitStoreDelegateConfig.getConnectorName())
                                                          .repoType(YAML)
                                                          .repoUrl(gitConfigDTO.getUrl())
                                                          .build();
    return gitClientV2.fetchFilesByPath(fetchFilesByPathRequest);
  }

  @Override
  public void downloadFiles(GitStoreDelegateConfig gitStoreDelegateConfig, String destinationDirectory,
      String accountId, SshSessionConfig sshSessionConfig, GitConfigDTO gitConfigDTO) {
    DownloadFilesRequest downloadFilesRequest = DownloadFilesRequest.builder()
                                                    .authRequest(getAuthRequest(gitConfigDTO, sshSessionConfig))
                                                    .filePaths(gitStoreDelegateConfig.getPaths())
                                                    .recursive(true)
                                                    .accountId(accountId)
                                                    .branch(gitStoreDelegateConfig.getBranch())
                                                    .commitId(gitStoreDelegateConfig.getCommitId())
                                                    .connectorId(gitStoreDelegateConfig.getConnectorName())
                                                    .repoType(YAML)
                                                    .repoUrl(gitConfigDTO.getUrl())
                                                    .destinationDirectory(destinationDirectory)
                                                    .build();
    gitClientV2.downloadFiles(downloadFilesRequest);
  }
}
