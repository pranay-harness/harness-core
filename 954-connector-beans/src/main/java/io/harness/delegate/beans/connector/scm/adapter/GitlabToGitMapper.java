package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitlabToGitMapper {
  public static GitConfig mapToGitConfigDTO(GitlabConnector gitlabConnector) {
    final GitAuthType authType = gitlabConnector.getAuthentication().getAuthType();
    final GitConnectionType connectionType = gitlabConnector.getConnectionType();
    final String url = gitlabConnector.getUrl();
    final String validationRepo = gitlabConnector.getValidationRepo();
    if (authType == GitAuthType.HTTP) {
      final GitlabHttpCredentials gitlabHttpCredentials =
          (GitlabHttpCredentials) gitlabConnector.getAuthentication().getCredentials();
      if (gitlabHttpCredentials.getType() == GitlabHttpAuthenticationType.KERBEROS) {
        // todo(Deepak): please add when we add kerboros support in generic git.
        throw new InvalidRequestException(
            "Git connector doesn't have configuration for " + gitlabHttpCredentials.getType());
      }
      String username;
      SecretRefData usernameRef, passwordRef;
      if (gitlabHttpCredentials.getType() == GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD) {
        final GitlabUsernamePassword httpCredentialsSpec =
            (GitlabUsernamePassword) gitlabHttpCredentials.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getPasswordRef();
      } else {
        final GitlabUsernameToken httpCredentialsSpec =
            (GitlabUsernameToken) gitlabHttpCredentials.getHttpCredentialsSpec();
        username = httpCredentialsSpec.getUsername();
        usernameRef = httpCredentialsSpec.getUsernameRef();
        passwordRef = httpCredentialsSpec.getTokenRef();
      }
      return GitConfigCreater.getGitConfigForHttp(connectionType, url, validationRepo, username, usernameRef,
          passwordRef, gitlabConnector.getDelegateSelectors());
    } else if (authType == GitAuthType.SSH) {
      final GitlabSshCredentials credentials =
          (GitlabSshCredentials) gitlabConnector.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = credentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(
          connectionType, url, validationRepo, sshKeyRef, gitlabConnector.getDelegateSelectors());
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
