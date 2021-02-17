package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitDTOToEntity implements ConnectorDTOToEntityMapper<GitConfigDTO, GitConfig> {
  private SecretRefService secretRefService;

  @Override
  public GitConfig toConnectorEntity(GitConfigDTO configDTO, NGAccess ngAccess) {
    GitConnectionType gitConnectionType = getGitConnectionLevel(configDTO);
    CustomCommitAttributes customCommitAttributes = getCustomCommitAttributes(configDTO);
    GitAuthentication gitAuthentication =
        getGitAuthentication(configDTO.getGitAuth(), configDTO.getGitAuthType(), ngAccess);
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

  private GitAuthentication getGitAuthentication(
      GitAuthenticationDTO gitAuthenticationDTO, GitAuthType gitAuthType, NGAccess ngAccess) {
    switch (gitAuthType) {
      case HTTP:
        return getHTTPGitAuthentication((GitHTTPAuthenticationDTO) gitAuthenticationDTO, ngAccess);
      case SSH:
        return getSSHGitAuthentication((GitSSHAuthenticationDTO) gitAuthenticationDTO, ngAccess);
      default:
        throw new UnknownEnumTypeException(
            "Git Authentication Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private GitUserNamePasswordAuthentication getHTTPGitAuthentication(
      GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO, NGAccess ngAccess) {
    return GitUserNamePasswordAuthentication.builder()
        .userName(gitHTTPAuthenticationDTO.getUsername())
        .userNameRef(
            secretRefService.validateAndGetSecretConfigString(gitHTTPAuthenticationDTO.getUsernameRef(), ngAccess))
        .passwordReference(
            secretRefService.validateAndGetSecretConfigString(gitHTTPAuthenticationDTO.getPasswordRef(), ngAccess))
        .build();
  }

  private GitSSHAuthentication getSSHGitAuthentication(
      GitSSHAuthenticationDTO gitSSHAuthenticationDTO, NGAccess ngAccess) {
    return GitSSHAuthentication.builder()
        .sshKeyReference(
            secretRefService.validateAndGetSecretConfigString(gitSSHAuthenticationDTO.getEncryptedSshKey(), ngAccess))
        .build();
  }

  private String getGitURL(GitConfigDTO gitConfig) {
    return gitConfig.getUrl();
  }
}
