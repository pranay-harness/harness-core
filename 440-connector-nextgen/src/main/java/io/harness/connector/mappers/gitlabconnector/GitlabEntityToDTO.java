package io.harness.connector.mappers.gitlabconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuth;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabTokenApiAccess;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthentication;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabKerberos;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentials;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernamePassword;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.govern.Switch;

@OwnedBy(HarnessTeam.DX)
public class GitlabEntityToDTO implements ConnectorEntityToDTOMapper<GitlabConnector,
    io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector> {
  @Override
  public GitlabConnector createConnectorDTO(
      io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector connector) {
    GitlabAuthentication gitlabAuthentication =
        buildGitlabAuthentication(connector.getAuthType(), connector.getAuthenticationDetails());
    GitlabApiAccess gitlabApiAccess = null;
    if (connector.isHasApiAccess()) {
      gitlabApiAccess = buildApiAccess(connector);
    }
    return GitlabConnector.builder()
        .apiAccess(gitlabApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(gitlabAuthentication)
        .url(connector.getUrl())
        .validationRepo(connector.getValidationRepo())
        .build();
  }

  public static GitlabAuthentication buildGitlabAuthentication(GitAuthType authType,
      io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication authenticationDetails) {
    GitlabCredentials gitlabCredentials = null;
    switch (authType) {
      case SSH:
        final GitlabSshAuthentication gitlabSshAuthentication = (GitlabSshAuthentication) authenticationDetails;
        gitlabCredentials = GitlabSshCredentials.builder()
                                .sshKeyRef(SecretRefHelper.createSecretRef(gitlabSshAuthentication.getSshKeyRef()))
                                .build();
        break;
      case HTTP:
        final GitlabHttpAuthentication gitlabHttpAuthentication = (GitlabHttpAuthentication) authenticationDetails;
        final GitlabHttpAuthenticationType type = gitlabHttpAuthentication.getType();
        final GitlabHttpAuth auth = gitlabHttpAuthentication.getAuth();
        GitlabHttpCredentialsSpec gitlabHttpCredentialsSpec = getHttpCredentialsSpecDTO(type, auth);
        gitlabCredentials =
            GitlabHttpCredentials.builder().type(type).httpCredentialsSpec(gitlabHttpCredentialsSpec).build();
        break;
      default:
        Switch.unhandled(authType);
    }
    return GitlabAuthentication.builder().authType(authType).credentials(gitlabCredentials).build();
  }

  private static GitlabHttpCredentialsSpec getHttpCredentialsSpecDTO(GitlabHttpAuthenticationType type, Object auth) {
    GitlabHttpCredentialsSpec gitlabHttpCredentialsSpec = null;
    switch (type) {
      case USERNAME_AND_TOKEN:
        final io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken usernameToken =
            (io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernameToken) auth;
        SecretRefData usernameReference = null;
        if (usernameToken.getUsernameRef() != null) {
          usernameReference = SecretRefHelper.createSecretRef(usernameToken.getUsernameRef());
        }
        gitlabHttpCredentialsSpec = GitlabUsernameToken.builder()
                                        .username(usernameToken.getUsername())
                                        .usernameRef(usernameReference)
                                        .tokenRef(SecretRefHelper.createSecretRef(usernameToken.getTokenRef()))
                                        .build();
        break;
      case USERNAME_AND_PASSWORD:
        final io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword gitlabUsernamePassword =
            (io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (gitlabUsernamePassword.getUsernameRef() != null) {
          usernameRef = SecretRefHelper.createSecretRef(gitlabUsernamePassword.getUsernameRef());
        }
        gitlabHttpCredentialsSpec =
            GitlabUsernamePassword.builder()
                .passwordRef(SecretRefHelper.createSecretRef(gitlabUsernamePassword.getPasswordRef()))
                .username(gitlabUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      case KERBEROS:
        final io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos gitlabKerberos =
            (io.harness.connector.entities.embedded.gitlabconnector.GitlabKerberos) auth;
        gitlabHttpCredentialsSpec =
            GitlabKerberos.builder()
                .kerberosKeyRef(SecretRefHelper.createSecretRef(gitlabKerberos.getKerberosKeyRef()))
                .build();
        break;
      default:
        Switch.unhandled(type);
    }
    return gitlabHttpCredentialsSpec;
  }

  private GitlabApiAccess buildApiAccess(
      io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector connector) {
    final GitlabTokenApiAccess gitlabTokenApiAccess = connector.getGitlabApiAccess();
    final GitlabTokenSpec gitlabTokenSpecDTO =
        GitlabTokenSpec.builder().tokenRef(SecretRefHelper.createSecretRef(gitlabTokenApiAccess.getTokenRef())).build();
    return GitlabApiAccess.builder().type(GitlabApiAccessType.TOKEN).spec(gitlabTokenSpecDTO).build();
  }
}
