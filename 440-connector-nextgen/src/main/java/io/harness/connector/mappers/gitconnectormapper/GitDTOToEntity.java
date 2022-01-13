/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
public class GitDTOToEntity implements ConnectorDTOToEntityMapper<GitConfigDTO, GitConfig> {
  @Override
  public GitConfig toConnectorEntity(GitConfigDTO configDTO) {
    GitConnectionType gitConnectionType = getGitConnectionLevel(configDTO);
    GitAuthentication gitAuthentication = getGitAuthentication(configDTO.getGitAuth(), configDTO.getGitAuthType());
    return GitConfig.builder()
        .connectionType(gitConnectionType)
        .url(getGitURL(configDTO))
        .validationRepo(configDTO.getValidationRepo())
        .authType(configDTO.getGitAuthType())
        .branchName(getBranchName(configDTO))
        .authenticationDetails(gitAuthentication)
        .build();
  }

  private String getBranchName(GitConfigDTO gitConfigDTO) {
    return gitConfigDTO.getBranchName();
  }

  private GitConnectionType getGitConnectionLevel(GitConfigDTO gitConfigDTO) {
    return gitConfigDTO.getGitConnectionType();
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
        .userNameRef(SecretRefHelper.getSecretConfigString(gitHTTPAuthenticationDTO.getUsernameRef()))
        .passwordReference(SecretRefHelper.getSecretConfigString(gitHTTPAuthenticationDTO.getPasswordRef()))
        .build();
  }

  private GitSSHAuthentication getSSHGitAuthentication(GitSSHAuthenticationDTO gitSSHAuthenticationDTO) {
    return GitSSHAuthentication.builder()
        .sshKeyReference(SecretRefHelper.getSecretConfigString(gitSSHAuthenticationDTO.getEncryptedSshKey()))
        .build();
  }

  private String getGitURL(GitConfigDTO gitConfig) {
    return gitConfig.getUrl();
  }
}
