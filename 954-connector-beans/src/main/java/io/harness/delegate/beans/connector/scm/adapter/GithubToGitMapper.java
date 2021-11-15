package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GithubToGitMapper {
  public static GitConfig mapToGitConfigDTO(GithubConnector githubConnector) {
    final GitAuthType authType = githubConnector.getAuthentication().getAuthType();
    final GitConnectionType connectionType = githubConnector.getConnectionType();
    final String url = githubConnector.getUrl();
    final String validationRepo = githubConnector.getValidationRepo();
    if (authType == GitAuthType.HTTP) {
      final GithubHttpCredentials credentials =
          (GithubHttpCredentials) githubConnector.getAuthentication().getCredentials();
      String username;
      SecretRefData usernameRef, passwordRef;
      if (credentials.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        final GithubUsernamePassword httpCredentialsSpec =
            (GithubUsernamePassword) credentials.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getPasswordRef();
      } else {
        final GithubUsernameToken githubUsernameToken = (GithubUsernameToken) credentials.getHttpCredentialsSpec();
        username = githubUsernameToken.getUsername();
        usernameRef = githubUsernameToken.getUsernameRef();
        passwordRef = githubUsernameToken.getTokenRef();
      }
      return GitConfigCreater.getGitConfigForHttp(connectionType, url, validationRepo, username, usernameRef,
          passwordRef, githubConnector.getDelegateSelectors());

    } else if (authType == GitAuthType.SSH) {
      final GithubSshCredentials credentials =
          (GithubSshCredentials) githubConnector.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(
          connectionType, url, validationRepo, sshKeyRef, githubConnector.getDelegateSelectors());
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
