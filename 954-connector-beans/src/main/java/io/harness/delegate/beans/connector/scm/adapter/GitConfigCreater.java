package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthentication;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthentication;
import io.harness.encryption.SecretRefData;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitConfigCreater {
  public static GitConfig getGitConfigForHttp(GitConnectionType gitConnectionType, String url, String validationRepo,
      String username, SecretRefData usernameRef, SecretRefData passwordRef, Set<String> delegateSelectors) {
    final GitHTTPAuthentication gitHTTPAuthenticationDTO =
        GitHTTPAuthentication.builder().passwordRef(passwordRef).username(username).usernameRef(usernameRef).build();
    return GitConfig.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.HTTP)
        .url(url)
        .validationRepo(validationRepo)
        .gitAuth(gitHTTPAuthenticationDTO)
        .delegateSelectors(delegateSelectors)
        .build();
  }

  public static GitConfig getGitConfigForSsh(GitConnectionType gitConnectionType, String url, String validationRepo,
      SecretRefData sshKey, Set<String> delegateSelectors) {
    final GitSSHAuthentication gitSSHAuthentication = GitSSHAuthentication.builder().encryptedSshKey(sshKey).build();
    return GitConfig.builder()
        .gitConnectionType(gitConnectionType)
        .gitAuthType(GitAuthType.SSH)
        .url(url)
        .validationRepo(validationRepo)
        .gitAuth(gitSSHAuthentication)
        .delegateSelectors(delegateSelectors)
        .build();
  }
}
