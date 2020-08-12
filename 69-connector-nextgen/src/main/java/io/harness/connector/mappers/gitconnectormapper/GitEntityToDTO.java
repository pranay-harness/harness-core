package io.harness.connector.mappers.gitconnectormapper;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.exception.UnknownEnumTypeException;

@Singleton
public class GitEntityToDTO implements ConnectorEntityToDTOMapper<GitConfig> {
  @Override
  public GitConfigDTO createConnectorDTO(GitConfig gitConnector) {
    GitAuthenticationDTO gitAuth = createGitAuthenticationDTO(gitConnector);
    GitSyncConfig gitSyncConfig = createGitSyncConfigDTO(gitConnector);
    return GitConfigDTO.builder()
        .gitAuthType(gitConnector.getAuthType())
        .gitAuth(gitAuth)
        .gitSyncConfig(gitSyncConfig)
        .build();
  }

  private GitAuthenticationDTO createGitAuthenticationDTO(GitConfig gitConfig) {
    switch (gitConfig.getAuthType()) {
      case HTTP:
        return createHTTPAuthenticationDTO(gitConfig);
      case SSH:
        return createSSHAuthenticationDTO(gitConfig);
      default:
        throw new UnknownEnumTypeException("Git Authentication Type",
            gitConfig.getAuthType() == null ? null : gitConfig.getAuthType().getDisplayName());
    }
  }

  private GitHTTPAuthenticationDTO createHTTPAuthenticationDTO(GitConfig gitConfig) {
    GitUserNamePasswordAuthentication userNamePasswordAuth =
        (GitUserNamePasswordAuthentication) gitConfig.getAuthenticationDetails();
    return GitHTTPAuthenticationDTO.builder()
        .gitConnectionType(gitConfig.getConnectionType())
        .url(gitConfig.getUrl())
        .username(userNamePasswordAuth.getUserName())
        .passwordRef(SecretRefHelper.createSecretRef(userNamePasswordAuth.getPasswordReference()))
        .branchName(gitConfig.getBranchName())
        .build();
  }

  private GitSSHAuthenticationDTO createSSHAuthenticationDTO(GitConfig gitConfig) {
    GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    return GitSSHAuthenticationDTO.builder()
        .gitConnectionType(gitConfig.getConnectionType())
        .url(gitConfig.getUrl())
        .encryptedSshKey(gitSSHAuthentication.getSshKeyReference())
        .branchName(gitConfig.getBranchName())
        .build();
  }

  private GitSyncConfig createGitSyncConfigDTO(GitConfig gitConnector) {
    return GitSyncConfig.builder()
        .isSyncEnabled(gitConnector.isSupportsGitSync())
        .customCommitAttributes(gitConnector.getCustomCommitAttributes())
        .build();
  }
}
