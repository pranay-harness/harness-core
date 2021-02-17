package io.harness.connector.mappers.githubconnector;

import io.harness.connector.entities.embedded.githubconnector.GithubApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubConnector;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuth;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword;
import io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
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
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GithubDTOToEntity implements ConnectorDTOToEntityMapper<GithubConnectorDTO, GithubConnector> {
  private SecretRefService secretRefService;

  @Override
  public GithubConnector toConnectorEntity(GithubConnectorDTO configDTO, NGAccess ngAccess) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    GithubAuthentication githubAuthentication =
        buildAuthenticationDetails(configDTO.getAuthentication().getCredentials(), gitAuthType, ngAccess);
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    GithubApiAccessType apiAccessType = null;
    GithubApiAccess githubApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      githubApiAccess = getApiAcessByType(configDTO.getApiAccess().getSpec(), apiAccessType, ngAccess);
    }
    return GithubConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .apiAccessType(apiAccessType)
        .authenticationDetails(githubAuthentication)
        .githubApiAccess(githubApiAccess)
        .url(configDTO.getUrl())
        .build();
  }

  private GithubAuthentication buildAuthenticationDetails(
      GithubCredentialsDTO credentialsDTO, GitAuthType gitAuthType, NGAccess ngAccess) {
    switch (gitAuthType) {
      case SSH:
        final GithubSshCredentialsDTO sshCredentialsDTO = (GithubSshCredentialsDTO) credentialsDTO;
        return GithubSshAuthentication.builder()
            .sshKeyRef(secretRefService.validateAndGetSecretConfigString(sshCredentialsDTO.getSshKeyRef(), ngAccess))
            .build();
      case HTTP:
        final GithubHttpCredentialsDTO httpCredentialsDTO = (GithubHttpCredentialsDTO) credentialsDTO;
        final GithubHttpAuthenticationType type = httpCredentialsDTO.getType();
        return GithubHttpAuthentication.builder()
            .type(type)
            .auth(getHttpAuth(type, httpCredentialsDTO, ngAccess))
            .build();
      default:
        throw new UnknownEnumTypeException(
            "Github Auth Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private GithubHttpAuth getHttpAuth(
      GithubHttpAuthenticationType type, GithubHttpCredentialsDTO httpCredentialsDTO, NGAccess ngAccess) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final GithubUsernamePasswordDTO usernamePasswordDTO =
            (GithubUsernamePasswordDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef(), ngAccess);
        return GithubUsernamePassword.builder()
            .passwordRef(
                secretRefService.validateAndGetSecretConfigString(usernamePasswordDTO.getPasswordRef(), ngAccess))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      case USERNAME_AND_TOKEN:
        final GithubUsernameTokenDTO githubUsernameTokenDTO =
            (GithubUsernameTokenDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameReference = getStringSecretForNullableSecret(githubUsernameTokenDTO.getUsernameRef(), ngAccess);
        return GithubUsernameToken.builder()
            .tokenRef(secretRefService.validateAndGetSecretConfigString(githubUsernameTokenDTO.getTokenRef(), ngAccess))
            .username(githubUsernameTokenDTO.getUsername())
            .usernameRef(usernameReference)
            .build();
      default:
        throw new UnknownEnumTypeException("Github Http Auth Type", type == null ? null : type.getDisplayName());
    }
  }
  private String getStringSecretForNullableSecret(SecretRefData secretRefData, NGAccess ngAccess) {
    String usernameRef = null;
    if (secretRefData != null) {
      usernameRef = secretRefService.validateAndGetSecretConfigString(secretRefData, ngAccess);
    }
    return usernameRef;
  }

  private GithubApiAccess getApiAcessByType(
      GithubApiAccessSpecDTO spec, GithubApiAccessType apiAccessType, NGAccess ngAccess) {
    switch (apiAccessType) {
      case TOKEN:
        final GithubTokenSpecDTO tokenSpec = (GithubTokenSpecDTO) spec;
        return GithubTokenApiAccess.builder()
            .tokenRef(secretRefService.validateAndGetSecretConfigString(tokenSpec.getTokenRef(), ngAccess))
            .build();
      case GITHUB_APP:
        final GithubAppSpecDTO githubAppSpecDTO = (GithubAppSpecDTO) spec;
        return GithubAppApiAccess.builder()
            .applicationId(githubAppSpecDTO.getApplicationId())
            .installationId(githubAppSpecDTO.getInstallationId())
            .privateKeyRef(
                secretRefService.validateAndGetSecretConfigString(githubAppSpecDTO.getPrivateKeyRef(), ngAccess))
            .build();
      default:
        throw new UnknownEnumTypeException(
            "Github Api Access Type", apiAccessType == null ? null : apiAccessType.getDisplayName());
    }
  }

  private GithubApiAccessType getApiAccessType(GithubApiAccessDTO apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(GithubApiAccessDTO apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(GithubAuthenticationDTO authentication) {
    return authentication.getAuthType();
  }
}
