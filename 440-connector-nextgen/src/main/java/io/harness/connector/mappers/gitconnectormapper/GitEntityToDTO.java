package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
public class GitEntityToDTO
    implements ConnectorEntityToDTOMapper<GitConfig, io.harness.connector.entities.embedded.gitconnector.GitConfig> {
  @Override
  public GitConfig createConnectorDTO(io.harness.connector.entities.embedded.gitconnector.GitConfig gitConnector) {
    GitAuthentication gitAuth = createGitAuthenticationDTO(gitConnector);
    return GitConfig.builder()
        .gitAuthType(gitConnector.getAuthType())
        .gitConnectionType(gitConnector.getConnectionType())
        .url(gitConnector.getUrl())
        .validationRepo(gitConnector.getValidationRepo())
        .branchName(gitConnector.getBranchName())
        .gitAuth(gitAuth)
        .build();
  }

  private GitAuthentication createGitAuthenticationDTO(
      io.harness.connector.entities.embedded.gitconnector.GitConfig gitConfig) {
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

  private GitHTTPAuthentication createHTTPAuthenticationDTO(
      io.harness.connector.entities.embedded.gitconnector.GitConfig gitConfig) {
    GitUserNamePasswordAuthentication userNamePasswordAuth =
        (GitUserNamePasswordAuthentication) gitConfig.getAuthenticationDetails();
    return GitHTTPAuthentication.builder()
        .username(userNamePasswordAuth.getUserName())
        .usernameRef(SecretRefHelper.createSecretRef(userNamePasswordAuth.getUserNameRef()))
        .passwordRef(SecretRefHelper.createSecretRef(userNamePasswordAuth.getPasswordReference()))
        .build();
  }

  private GitSSHAuthentication createSSHAuthenticationDTO(
      io.harness.connector.entities.embedded.gitconnector.GitConfig gitConfig) {
    io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication gitSSHAuthentication =
        (io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication) gitConfig.getAuthenticationDetails();
    return GitSSHAuthentication.builder()
        .encryptedSshKey(SecretRefHelper.createSecretRef(gitSSHAuthentication.getSshKeyReference()))
        .build();
  }
}
