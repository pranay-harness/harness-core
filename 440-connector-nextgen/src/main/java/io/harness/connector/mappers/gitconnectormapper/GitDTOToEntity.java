package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class GitDTOToEntity implements ConnectorDTOToEntityMapper<GitConfigDTO> {
  @Override
  public GitConfig toConnectorEntity(GitConfigDTO configDTO) {
    GitConnectionType gitConnectionType = getGitConnectionLevel(configDTO);
    CustomCommitAttributes customCommitAttributes = getCustomCommitAttributes(configDTO);
    GitAuthentication gitAuthentication = getGitAuthentication(configDTO.getGitAuth(), configDTO.getGitAuthType());
    boolean isGitSyncSupported = isGitSyncSupported(configDTO);
    return GitConfig.builder()
        .connectionType(gitConnectionType)
        .url(getGitURL(configDTO))
        .authType(configDTO.getGitAuthType())
        .supportsGitSync(isGitSyncSupported)
        .branchName(getBranchName(configDTO))
        .customCommitAttributes(customCommitAttributes)
        .authenticationDetails(gitAuthentication)
        .build();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.CODE_REPO);
  }

  private String getBranchName(GitConfigDTO gitConfigDTO) {
    return gitConfigDTO.getBranchName();
  }

  private GitConnectionType getGitConnectionLevel(GitConfigDTO gitConfigDTO) {
    return gitConfigDTO.getGitConnectionType();
  }

  private boolean isGitSyncSupported(GitConfigDTO gitConfigDTO) {
    if (gitConfigDTO.getGitSyncConfig() != null) {
      return gitConfigDTO.getGitSyncConfig().isSyncEnabled();
    }
    return false;
  }

  private CustomCommitAttributes getCustomCommitAttributes(GitConfigDTO configDTO) {
    if (configDTO.getGitSyncConfig() != null) {
      return configDTO.getGitSyncConfig().getCustomCommitAttributes();
    }
    return null;
  }

  private GitAuthentication getGitAuthentication(GitAuthenticationDTO gitAuthenticationDTO, GitAuthType gitAuthType) {
    switch (gitAuthType) {
      case HTTP:
        return getHTTPGitAuthentication((GitHTTPAuthenticationDTO) gitAuthenticationDTO);
      case SSH:
        return getSSHGitAuthentication((GitSSHAuthenticationDTO) gitAuthenticationDTO);
      default:
        throw new UnknownEnumTypeException(
            "Git Authentication Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private GitUserNamePasswordAuthentication getHTTPGitAuthentication(
      GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO) {
    return GitUserNamePasswordAuthentication.builder()
        .userName(gitHTTPAuthenticationDTO.getUsername())
        .passwordReference(SecretRefHelper.getSecretConfigString(gitHTTPAuthenticationDTO.getPasswordRef()))
        .build();
  }

  private GitSSHAuthentication getSSHGitAuthentication(GitSSHAuthenticationDTO gitSSHAuthenticationDTO) {
    return GitSSHAuthentication.builder().sshKeyReference(gitSSHAuthenticationDTO.getEncryptedSshKey()).build();
  }

  private String getGitURL(GitConfigDTO gitConfig) {
    return gitConfig.getUrl();
  }
}
