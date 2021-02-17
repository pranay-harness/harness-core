package io.harness.connector.mappers.githubconnector;

import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubConnector;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuth;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.govern.Switch;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GithubEntityToDTO implements ConnectorEntityToDTOMapper<GithubConnectorDTO, GithubConnector> {
  private SecretRefService secretRefService;

  @Override
  public GithubConnectorDTO createConnectorDTO(GithubConnector connector) {
    GithubAuthenticationDTO githubAuthenticationDTO = buildGithubAuthentication(connector);
    GithubApiAccessDTO githubApiAccess = null;
    if (connector.isHasApiAccess()) {
      githubApiAccess = buildApiAccess(connector);
    }
    return GithubConnectorDTO.builder()
        .apiAccess(githubApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(githubAuthenticationDTO)
        .url(connector.getUrl())
        .build();
  }

  private GithubAuthenticationDTO buildGithubAuthentication(GithubConnector connector) {
    final GitAuthType authType = connector.getAuthType();
    final GithubAuthentication authenticationDetails = connector.getAuthenticationDetails();
    GithubCredentialsDTO githubCredentialsDTO = null;
    switch (authType) {
      case SSH:
        final GithubSshAuthentication githubSshAuthentication = (GithubSshAuthentication) authenticationDetails;
        githubCredentialsDTO = GithubSshCredentialsDTO.builder()
                                   .sshKeyRef(secretRefService.createSecretRef(githubSshAuthentication.getSshKeyRef()))
                                   .build();
        break;
      case HTTP:
        final GithubHttpAuthentication githubHttpAuthentication = (GithubHttpAuthentication) authenticationDetails;
        final GithubHttpAuthenticationType type = githubHttpAuthentication.getType();
        final GithubHttpAuth auth = githubHttpAuthentication.getAuth();
        GithubHttpCredentialsSpecDTO githubHttpCredentialsSpecDTO = getHttpCredentialsSpecDTO(type, auth);
        githubCredentialsDTO =
            GithubHttpCredentialsDTO.builder().type(type).httpCredentialsSpec(githubHttpCredentialsSpecDTO).build();
        break;
      default:
        Switch.unhandled(authType);
    }
    return GithubAuthenticationDTO.builder().authType(authType).credentials(githubCredentialsDTO).build();
  }

  private GithubHttpCredentialsSpecDTO getHttpCredentialsSpecDTO(GithubHttpAuthenticationType type, Object auth) {
    GithubHttpCredentialsSpecDTO githubHttpCredentialsSpecDTO = null;
    switch (type) {
      case USERNAME_AND_TOKEN:
        final GithubUsernameToken usernameToken = (GithubUsernameToken) auth;
        SecretRefData usernameReference = null;
        if (usernameToken.getUsernameRef() != null) {
          usernameReference = secretRefService.createSecretRef(usernameToken.getUsernameRef());
        }
        githubHttpCredentialsSpecDTO = GithubUsernameTokenDTO.builder()
                                           .username(usernameToken.getUsername())
                                           .usernameRef(usernameReference)
                                           .tokenRef(secretRefService.createSecretRef(usernameToken.getTokenRef()))
                                           .build();
        break;
      case USERNAME_AND_PASSWORD:
        final GithubUsernamePassword githubUsernamePassword = (GithubUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (githubUsernamePassword.getUsernameRef() != null) {
          usernameRef = secretRefService.createSecretRef(githubUsernamePassword.getUsernameRef());
        }
        githubHttpCredentialsSpecDTO =
            GithubUsernamePasswordDTO.builder()
                .passwordRef(secretRefService.createSecretRef(githubUsernamePassword.getPasswordRef()))
                .username(githubUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      default:
        Switch.unhandled(type);
    }
    return githubHttpCredentialsSpecDTO;
  }

  private GithubApiAccessDTO buildApiAccess(GithubConnector connector) {
    final GithubApiAccessType apiAccessType = connector.getApiAccessType();
    GithubApiAccessSpecDTO apiAccessSpecDTO = null;
    switch (apiAccessType) {
      case GITHUB_APP:
        final GithubAppApiAccess githubApiAccess = (GithubAppApiAccess) connector.getGithubApiAccess();
        apiAccessSpecDTO = GithubAppSpecDTO.builder()
                               .applicationId(githubApiAccess.getApplicationId())
                               .installationId(githubApiAccess.getInstallationId())
                               .privateKeyRef(secretRefService.createSecretRef(githubApiAccess.getPrivateKeyRef()))
                               .build();
        break;
      case TOKEN:
        final GithubTokenApiAccess githubTokenApiAccess = (GithubTokenApiAccess) connector.getGithubApiAccess();
        apiAccessSpecDTO = GithubTokenSpecDTO.builder()
                               .tokenRef(secretRefService.createSecretRef(githubTokenApiAccess.getTokenRef()))
                               .build();
        break;
      default:
        Switch.unhandled(apiAccessType);
    }
    return GithubApiAccessDTO.builder().type(apiAccessType).spec(apiAccessSpecDTO).build();
  }
}
