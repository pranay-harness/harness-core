package io.harness.connector.scmMappers;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class GitConfigCreater {
  public GitConfigDTO getGitConfigForHttp(GitConnectionType gitConnectionType, String url, String username,
      SecretRefData usernameRef, SecretRefData passwordRef) {
    if (usernameRef != null) {
      throw new InvalidRequestException("Username Ref not implemented for git");
    }
    final GitHTTPAuthenticationDTO gitHTTPAuthenticationDTO =
        GitHTTPAuthenticationDTO.builder().passwordRef(passwordRef).username(username).build();
    return GitConfigDTO.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.HTTP)
        .url(url)
        .gitAuth(gitHTTPAuthenticationDTO)
        .build();
  }

  public GitConfigDTO getGitConfigForSsh(GitConnectionType gitConnectionType, String url, SecretRefData sshKey) {
    final GitSSHAuthenticationDTO gitSSHAuthenticationDTO =
        GitSSHAuthenticationDTO.builder().encryptedSshKey(sshKey).build().builder().build();
    return GitConfigDTO.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.SSH)
        .url(url)
        .gitAuth(gitSSHAuthenticationDTO)
        .build();
  }
}
