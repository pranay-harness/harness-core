package io.harness.connector.scmMappers;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
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
      if (credentials.getType() != GithubHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        throw new InvalidRequestException("Git connector doesn't have configuration for " + credentials.getType());
      }
      final GithubUsernamePasswordDTO httpCredentialsSpec =
          (GithubUsernamePasswordDTO) credentials.getHttpCredentialsSpec();
      return gitConfigCreater.getGitConfigForHttp(connectionType, url, httpCredentialsSpec.getUsername(),
          httpCredentialsSpec.getUsernameRef(), httpCredentialsSpec.getPasswordRef());
    } else if (authType == GitAuthType.SSH) {
      final GithubSshCredentialsDTO credentials =
          (GithubSshCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSpec().getSshKeyRef();
      return gitConfigCreater.getGitConfigForSsh(connectionType, url, sshKeyRef);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
