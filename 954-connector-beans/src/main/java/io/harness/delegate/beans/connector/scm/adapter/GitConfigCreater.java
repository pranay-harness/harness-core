/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.SecretRefData;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitConfigCreater {
  public static GitConfigDTO getGitConfigForHttp(GitConnectionType gitConnectionType, String url, String validationRepo,
      String username, SecretRefData usernameRef, SecretRefData passwordRef, Set<String> delegateSelectors) {
    final GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO =
        GitHTTPAuthenticationDTO.builder().passwordRef(passwordRef).username(username).usernameRef(usernameRef).build();
    return GitConfigDTO.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.HTTP)
        .url(url)
        .validationRepo(validationRepo)
        .gitAuth(gitHTTPAuthenticationDTO)
        .delegateSelectors(delegateSelectors)
        .build();
  }

  public static GitConfigDTO getGitConfigForSsh(GitConnectionType gitConnectionType, String url, String validationRepo,
      SecretRefData sshKey, Set<String> delegateSelectors) {
    final GitSSHAuthenticationDTO gitSSHAuthenticationDTO =
        GitSSHAuthenticationDTO.builder().encryptedSshKey(sshKey).build();
    return GitConfigDTO.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.SSH)
        .url(url)
        .validationRepo(validationRepo)
        .gitAuth(gitSSHAuthenticationDTO)
        .delegateSelectors(delegateSelectors)
        .build();
  }
}
