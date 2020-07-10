package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.SSH;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.gitconnector.GitAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.UserNamePasswordGitAuthentication;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.exception.InvalidRequestException;

@Singleton
public class GitDTOToEntity {
  public GitConfig toGitConfig(GitConfigDTO configDTO) {
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

  private String getBranchName(GitConfigDTO gitConfigDTO) {
    switch (gitConfigDTO.getGitAuthType()) {
      case HTTP:
        return ((GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth()).getBranchName();
      case SSH:
        return ((GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth()).getBranchName();
      default:
        throw new InvalidRequestException(
            String.format("The git auth type  %s doesn't exists", gitConfigDTO.getGitAuth()));
    }
  }

  private GitConnectionType getGitConnectionLevel(GitConfigDTO gitConfigDTO) {
    switch (gitConfigDTO.getGitAuthType()) {
      case HTTP:
        return ((GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth()).getGitType();
      case SSH:
        return ((GitSSHAuthenticationDTO) gitConfigDTO.getGitAuth()).getGitType();
      default:
        throw new InvalidRequestException(
            String.format("The git auth type  %s doesn't exists", gitConfigDTO.getGitAuth()));
    }
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
    // todo: @deepak Have some different design pattern, should have swich case here also
    switch (gitAuthType) {
      case HTTP:
        return getHTTPGitAuthentication((GitHTTPAuthenticationDTO) gitAuthenticationDTO);
      case SSH:
        return getSSHGitAuthentication((GitSSHAuthenticationDTO) gitAuthenticationDTO);
      default:
        throw new InvalidRequestException(String.format("The git auth type %s doesn't exists", gitAuthType));
    }
  }

  private UserNamePasswordGitAuthentication getHTTPGitAuthentication(
      GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO) {
    return UserNamePasswordGitAuthentication.builder()
        .userName(gitHTTPAuthenticationDTO.getUsername())
        .passwordReference(gitHTTPAuthenticationDTO.getPasswordReference())
        .build();
  }

  private GitSSHAuthentication getSSHGitAuthentication(GitSSHAuthenticationDTO gitSSHAuthenticationDTO) {
    return GitSSHAuthentication.builder().sshKeyReference(gitSSHAuthenticationDTO.getSshKeyReference()).build();
  }

  private String getGitURL(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case HTTP:
        return ((GitHTTPAuthenticationDTO) gitConfig.getGitAuth()).getUrl();
      case SSH:
        return ((GitSSHAuthenticationDTO) gitConfig.getGitAuth()).getUrl();
      default:
        throw new InvalidRequestException(String.format("The git level %s doesn't exists", gitConfig.getGitAuth()));
    }
  }
}
