package io.harness.connector.scmMappers;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GithubToGitMapper {
  @Inject GitConfigCreater gitConfigCreater;

  public GitConfigDTO mapToGitConfigDTO(GithubConnectorDTO githubConnectorDTO) {
    final GitAuthType authType = githubConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = githubConnectorDTO.getConnectionType();
    final String url = githubConnectorDTO.getUrl();
    if (authType == GitAuthType.HTTP) {
      final GithubHttpCredentialsDTO credentials =
          (GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();
      String username;
      SecretRefData usernameRef, passwordRef;
      if (credentials.getType() == GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        final GithubUsernamePasswordDTO httpCredentialsSpec =
            (GithubUsernamePasswordDTO) credentials.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getPasswordRef();
      } else {
        final GithubUsernameTokenDTO githubUsernameTokenDTO =
            (GithubUsernameTokenDTO) credentials.getHttpCredentialsSpec();
        username = githubUsernameTokenDTO.getUsername();
        usernameRef = githubUsernameTokenDTO.getUsernameRef();
        passwordRef = githubUsernameTokenDTO.getTokenRef();
      }
      return gitConfigCreater.getGitConfigForHttp(connectionType, url, username, usernameRef, passwordRef);

    } else if (authType == GitAuthType.SSH) {
      final GithubSshCredentialsDTO credentials =
          (GithubSshCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSpec().getSshKeyRef();
      return gitConfigCreater.getGitConfigForSsh(connectionType, url, sshKeyRef);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
