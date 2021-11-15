package io.harness.connector.mappers.githubconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuth;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpec;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubAuthentication;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.github.GithubCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsSpec;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpec;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.govern.Switch;

@OwnedBy(HarnessTeam.DX)
public class GithubEntityToDTO implements ConnectorEntityToDTOMapper<GithubConnector,
    io.harness.connector.entities.embedded.githubconnector.GithubConnector> {
  @Override
  public GithubConnector createConnectorDTO(
      io.harness.connector.entities.embedded.githubconnector.GithubConnector connector) {
    GithubAuthentication githubAuthentication =
        buildGithubAuthentication(connector.getAuthType(), connector.getAuthenticationDetails());
    GithubApiAccess githubApiAccess = null;
    if (connector.isHasApiAccess()) {
      githubApiAccess = buildApiAccess(connector);
    }
    return GithubConnector.builder()
        .apiAccess(githubApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(githubAuthentication)
        .url(connector.getUrl())
        .validationRepo(connector.getValidationRepo())
        .build();
  }

  public static GithubAuthentication buildGithubAuthentication(GitAuthType authType,
      io.harness.connector.entities.embedded.githubconnector.GithubAuthentication authenticationDetails) {
    GithubCredentials githubCredentialsDTO = null;
    switch (authType) {
      case SSH:
        final GithubSshAuthentication githubSshAuthentication = (GithubSshAuthentication) authenticationDetails;
        githubCredentialsDTO = GithubSshCredentials.builder()
                                   .sshKeyRef(SecretRefHelper.createSecretRef(githubSshAuthentication.getSshKeyRef()))
                                   .build();
        break;
      case HTTP:
        final GithubHttpAuthentication githubHttpAuthentication = (GithubHttpAuthentication) authenticationDetails;
        final GithubHttpAuthenticationType type = githubHttpAuthentication.getType();
        final GithubHttpAuth auth = githubHttpAuthentication.getAuth();
        GithubHttpCredentialsSpec githubHttpCredentialsSpec = getHttpCredentialsSpecDTO(type, auth);
        githubCredentialsDTO =
            GithubHttpCredentials.builder().type(type).httpCredentialsSpec(githubHttpCredentialsSpec).build();
        break;
      default:
        Switch.unhandled(authType);
    }
    return GithubAuthentication.builder().authType(authType).credentials(githubCredentialsDTO).build();
  }

  private static GithubHttpCredentialsSpec getHttpCredentialsSpecDTO(GithubHttpAuthenticationType type, Object auth) {
    GithubHttpCredentialsSpec githubHttpCredentialsSpecDTO = null;
    switch (type) {
      case USERNAME_AND_TOKEN:
        final io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken usernameToken =
            (io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken) auth;
        SecretRefData usernameReference = null;
        if (usernameToken.getUsernameRef() != null) {
          usernameReference = SecretRefHelper.createSecretRef(usernameToken.getUsernameRef());
        }
        githubHttpCredentialsSpecDTO = GithubUsernameToken.builder()
                                           .username(usernameToken.getUsername())
                                           .usernameRef(usernameReference)
                                           .tokenRef(SecretRefHelper.createSecretRef(usernameToken.getTokenRef()))
                                           .build();
        break;
      case USERNAME_AND_PASSWORD:
        final io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword githubUsernamePassword =
            (io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (githubUsernamePassword.getUsernameRef() != null) {
          usernameRef = SecretRefHelper.createSecretRef(githubUsernamePassword.getUsernameRef());
        }
        githubHttpCredentialsSpecDTO =
            GithubUsernamePassword.builder()
                .passwordRef(SecretRefHelper.createSecretRef(githubUsernamePassword.getPasswordRef()))
                .username(githubUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      default:
        Switch.unhandled(type);
    }
    return githubHttpCredentialsSpecDTO;
  }

  private GithubApiAccess buildApiAccess(
      io.harness.connector.entities.embedded.githubconnector.GithubConnector connector) {
    final GithubApiAccessType apiAccessType = connector.getApiAccessType();
    GithubApiAccessSpec apiAccessSpecDTO = null;
    switch (apiAccessType) {
      case GITHUB_APP:
        final GithubAppApiAccess githubApiAccess = (GithubAppApiAccess) connector.getGithubApiAccess();
        apiAccessSpecDTO = GithubAppSpec.builder()
                               .applicationId(githubApiAccess.getApplicationId())
                               .installationId(githubApiAccess.getInstallationId())
                               .privateKeyRef(SecretRefHelper.createSecretRef(githubApiAccess.getPrivateKeyRef()))
                               .build();
        break;
      case TOKEN:
        final GithubTokenApiAccess githubTokenApiAccess = (GithubTokenApiAccess) connector.getGithubApiAccess();
        apiAccessSpecDTO = GithubTokenSpec.builder()
                               .tokenRef(SecretRefHelper.createSecretRef(githubTokenApiAccess.getTokenRef()))
                               .build();
        break;
      default:
        Switch.unhandled(apiAccessType);
    }
    return GithubApiAccess.builder().type(apiAccessType).spec(apiAccessSpecDTO).build();
  }
}
