package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
public class GitDTOToEntity
    implements ConnectorDTOToEntityMapper<GitConfig, io.harness.connector.entities.embedded.gitconnector.GitConfig> {
  @Override
  public io.harness.connector.entities.embedded.gitconnector.GitConfig toConnectorEntity(GitConfig configDTO) {
    GitConnectionType gitConnectionType = getGitConnectionLevel(configDTO);
    io.harness.connector.entities.embedded.gitconnector.GitAuthentication gitAuthentication =
        getGitAuthentication(configDTO.getGitAuth(), configDTO.getGitAuthType());
    return io.harness.connector.entities.embedded.gitconnector.GitConfig.builder()
        .connectionType(gitConnectionType)
        .url(getGitURL(configDTO))
        .validationRepo(configDTO.getValidationRepo())
        .authType(configDTO.getGitAuthType())
        .branchName(getBranchName(configDTO))
        .authenticationDetails(gitAuthentication)
        .build();
  }

  private String getBranchName(GitConfig gitConfig) {
    return gitConfig.getBranchName();
  }

  private GitConnectionType getGitConnectionLevel(GitConfig gitConfig) {
    return gitConfig.getGitConnectionType();
  }

  private io.harness.connector.entities.embedded.gitconnector.GitAuthentication getGitAuthentication(
      GitAuthentication gitAuthentication, GitAuthType gitAuthType) {
    switch (gitAuthType) {
      case HTTP:
        return getHTTPGitAuthentication((GitHTTPAuthentication) gitAuthentication);
      case SSH:
        return getSSHGitAuthentication((GitSSHAuthentication) gitAuthentication);
      default:
        throw new UnknownEnumTypeException(
            "Git Authentication Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private GitUserNamePasswordAuthentication getHTTPGitAuthentication(GitHTTPAuthentication gitHTTPAuthenticationDTO) {
    return GitUserNamePasswordAuthentication.builder()
        .userName(gitHTTPAuthenticationDTO.getUsername())
        .userNameRef(SecretRefHelper.getSecretConfigString(gitHTTPAuthenticationDTO.getUsernameRef()))
        .passwordReference(SecretRefHelper.getSecretConfigString(gitHTTPAuthenticationDTO.getPasswordRef()))
        .build();
  }

  private io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication getSSHGitAuthentication(
      GitSSHAuthentication gitSSHAuthentication) {
    return io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication.builder()
        .sshKeyReference(SecretRefHelper.getSecretConfigString(gitSSHAuthentication.getEncryptedSshKey()))
        .build();
  }

  private String getGitURL(GitConfig gitConfig) {
    return gitConfig.getUrl();
  }
}
