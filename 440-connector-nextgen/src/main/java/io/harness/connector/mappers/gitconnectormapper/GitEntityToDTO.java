/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.gitconnectormapper;

import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
public class GitEntityToDTO implements ConnectorEntityToDTOMapper<GitConfigDTO, GitConfig> {
  @Override
  public GitConfigDTO createConnectorDTO(GitConfig gitConnector) {
    GitAuthenticationDTO gitAuth = createGitAuthenticationDTO(gitConnector);
    return GitConfigDTO.builder()
        .gitAuthType(gitConnector.getAuthType())
        .gitConnectionType(gitConnector.getConnectionType())
        .url(gitConnector.getUrl())
        .validationRepo(gitConnector.getValidationRepo())
        .branchName(gitConnector.getBranchName())
        .gitAuth(gitAuth)
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
}
