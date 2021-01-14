package io.harness.connector.scmMappers;

import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitlabToGitMapper {
  @Inject GitConfigCreater gitConfigCreater;

  public GitConfigDTO mapToGitConfigDTO(GitlabConnectorDTO gitlabConnectorDTO) {
    final GitAuthType authType = gitlabConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = gitlabConnectorDTO.getConnectionType();
    final String url = gitlabConnectorDTO.getUrl();
    if (authType == GitAuthType.HTTP) {
      final GitlabHttpCredentialsDTO gitlabHttpCredentialsDTO =
          (GitlabHttpCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials();
      if (gitlabHttpCredentialsDTO.getType() == GitlabHttpAuthenticationType.KERBEROS) {
        throw new InvalidRequestException(
            "Git connector doesn't have configuration for " + gitlabHttpCredentialsDTO.getType());
      }
      String username;
      SecretRefData usernameRef, passwordRef;
      if (gitlabHttpCredentialsDTO.getType() == GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        final GitlabUsernamePasswordDTO httpCredentialsSpec =
            (GitlabUsernamePasswordDTO) gitlabHttpCredentialsDTO.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getPasswordRef();
      } else {
        final GitlabUsernameTokenDTO httpCredentialsSpec =
            (GitlabUsernameTokenDTO) gitlabHttpCredentialsDTO.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getTokenRef();
      }
      return gitConfigCreater.getGitConfigForHttp(connectionType, url, username, usernameRef, passwordRef);
    } else if (authType == GitAuthType.SSH) {
      final GitlabSshCredentialsDTO credentials =
          (GitlabSshCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSpec().getSshKeyRef();
      return gitConfigCreater.getGitConfigForSsh(connectionType, url, sshKeyRef);
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
