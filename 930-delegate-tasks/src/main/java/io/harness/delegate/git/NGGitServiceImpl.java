package io.harness.delegate.git;

import static io.harness.git.model.GitRepositoryType.YAML;

import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@ValidateOnExecution
@Slf4j
public class NGGitServiceImpl implements NGGitService {
  @Inject private GitClientV2 gitClientV2;

  @Override
  public String validate(GitConfigDTO gitConfig, String accountId) {
    final GitBaseRequest gitBaseRequest = GitBaseRequest.builder().build();
    setGitBaseRequest(gitConfig, accountId, gitBaseRequest, YAML);
    return gitClientV2.validate(gitBaseRequest);
  }

  @VisibleForTesting
  void setGitBaseRequest(
      GitConfigDTO gitConfig, String accountId, GitBaseRequest gitBaseRequest, GitRepositoryType repositoryType) {
    gitBaseRequest.setAuthRequest(getAuthRequest(gitConfig));
    gitBaseRequest.setBranch(gitConfig.getBranchName());
    gitBaseRequest.setRepoType(repositoryType);
    gitBaseRequest.setRepoUrl(gitConfig.getUrl());
    gitBaseRequest.setAccountId(accountId);
  }

  private AuthRequest getAuthRequest(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case SSH:
        // todo @deepak @vaibhav: handle ssh auth when added for ng.
        throw new NotImplementedException("Ssh not yet implemented");
      case HTTP:
        // todo @deepak @vaibhav: handle kerboros when added.
        GitHTTPAuthenticationDTO httpAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        return UsernamePasswordAuthRequest.builder()
            .username(httpAuthenticationDTO.getUsername())
            .password(httpAuthenticationDTO.getPasswordRef().getDecryptedValue())
            .build();
      default:
        throw new InvalidRequestException("Unknown auth type.");
    }
  }

  private void setAuthorInfo(GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest) {
    final Optional<String> name = Optional.ofNullable(gitConfig.getGitSyncConfig())
                                      .map(GitSyncConfig::getCustomCommitAttributes)
                                      .map(CustomCommitAttributes::getAuthorName);
    final Optional<String> email = Optional.ofNullable(gitConfig.getGitSyncConfig())
                                       .map(GitSyncConfig::getCustomCommitAttributes)
                                       .map(CustomCommitAttributes::getAuthorEmail);
    final Optional<String> message = Optional.ofNullable(gitConfig.getGitSyncConfig())
                                         .map(GitSyncConfig::getCustomCommitAttributes)
                                         .map(CustomCommitAttributes::getCommitMessage);

    name.ifPresent(commitAndPushRequest::setAuthorName);
    email.ifPresent(commitAndPushRequest::setAuthorEmail);
    message.ifPresent(commitAndPushRequest::setCommitMessage);
  }

  @Override
  public CommitAndPushResult commitAndPush(
      GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest, String accountId) {
    setGitBaseRequest(gitConfig, accountId, commitAndPushRequest, YAML);
    setAuthorInfo(gitConfig, commitAndPushRequest);
    return gitClientV2.commitAndPush(commitAndPushRequest);
  }

  @Override
  public FetchFilesResult fetchFilesByPath(GitStoreDelegateConfig gitStoreDelegateConfig, String accountId) {
    GitConfigDTO gitConfigDTO = gitStoreDelegateConfig.getGitConfigDTO();
    FetchFilesByPathRequest fetchFilesByPathRequest = FetchFilesByPathRequest.builder()
                                                          .authRequest(getAuthRequest(gitConfigDTO))
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
  public void downloadFiles(
      GitStoreDelegateConfig gitStoreDelegateConfig, String destinationDirectory, String accountId) {
    GitConfigDTO gitConfigDTO = gitStoreDelegateConfig.getGitConfigDTO();
    DownloadFilesRequest downloadFilesRequest = DownloadFilesRequest.builder()
                                                    .authRequest(getAuthRequest(gitConfigDTO))
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
