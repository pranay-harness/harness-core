package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSyncConfig;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class GitEntityToDTO implements ConnectorEntityToDTOMapper<GitConfigDTO, GitConfig> {
  @Override
  public GitConfigDTO createConnectorDTO(GitConfig gitConnector) {
    GitAuthenticationDTO gitAuth = createGitAuthenticationDTO(gitConnector);
    GitSyncConfig gitSyncConfig = createGitSyncConfigDTO(gitConnector);
    return GitConfigDTO.builder()
        .gitAuthType(gitConnector.getAuthType())
        .gitConnectionType(gitConnector.getConnectionType())
        .url(gitConnector.getUrl())
        .branchName(gitConnector.getBranchName())
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
        .username(userNamePasswordAuth.getUserName())
        .usernameRef(SecretRefHelper.createSecretRef(userNamePasswordAuth.getUserNameRef()))
        .passwordRef(SecretRefHelper.createSecretRef(userNamePasswordAuth.getPasswordReference()))
        .build();
  }

  private GitSSHAuthenticationDTO createSSHAuthenticationDTO(GitConfig gitConfig) {
    GitSSHAuthentication gitSSHAuthentication = (GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    return GitSSHAuthenticationDTO.builder()
        .encryptedSshKey(SecretRefHelper.createSecretRef(gitSSHAuthentication.getSshKeyReference()))
        .build();
  }

  private GitSyncConfig createGitSyncConfigDTO(GitConfig gitConnector) {
    return GitSyncConfig.builder()
        .isSyncEnabled(gitConnector.isSupportsGitSync())
        .customCommitAttributes(gitConnector.getCustomCommitAttributes())
        .build();
  }
}
