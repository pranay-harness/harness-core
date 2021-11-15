package io.harness.connector.mappers.githubconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.githubconnector.GithubApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubConnector;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuth;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
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
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

@OwnedBy(HarnessTeam.DX)
public class GithubDTOToEntity implements ConnectorDTOToEntityMapper<GithubConnectorDTO, GithubConnector> {
  @Override
  public GithubConnector toConnectorEntity(GithubConnectorDTO configDTO) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    GithubAuthentication githubAuthentication =
        buildAuthenticationDetails(gitAuthType, configDTO.getAuthentication().getCredentials());
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    GithubApiAccessType apiAccessType = null;
    GithubApiAccess githubApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      githubApiAccess = getApiAcessByType(configDTO.getApiAccess().getSpec(), apiAccessType);
    }
    return GithubConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .apiAccessType(apiAccessType)
        .authenticationDetails(githubAuthentication)
        .githubApiAccess(githubApiAccess)
        .url(configDTO.getUrl())
        .validationRepo(configDTO.getValidationRepo())
        .build();
  }

  public static GithubAuthentication buildAuthenticationDetails(
      GitAuthType gitAuthType, GithubCredentialsDTO credentialsDTO) {
    switch (gitAuthType) {
      case SSH:
        final GithubSshCredentialsDTO sshCredentialsDTO = (GithubSshCredentialsDTO) credentialsDTO;
        return GithubSshAuthentication.builder()
            .sshKeyRef(SecretRefHelper.getSecretConfigString(sshCredentialsDTO.getSshKeyRef()))
            .build();
      case HTTP:
        final GithubHttpCredentialsDTO httpCredentialsDTO = (GithubHttpCredentialsDTO) credentialsDTO;
        final GithubHttpAuthenticationType type = httpCredentialsDTO.getType();
        return GithubHttpAuthentication.builder().type(type).auth(getHttpAuth(type, httpCredentialsDTO)).build();
      default:
        throw new UnknownEnumTypeException(
            "Github Auth Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private static GithubHttpAuth getHttpAuth(
      GithubHttpAuthenticationType type, GithubHttpCredentialsDTO httpCredentialsDTO) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final GithubUsernamePassword usernamePasswordDTO =
            (GithubUsernamePassword) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef());
        return io.harness.connector.entities.embedded.githubconnector.GithubUsernamePassword.builder()
            .passwordRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getPasswordRef()))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      case USERNAME_AND_TOKEN:
        final GithubUsernameToken githubUsernameToken =
            (GithubUsernameToken) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameReference = getStringSecretForNullableSecret(githubUsernameToken.getUsernameRef());
        return io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken.builder()
            .tokenRef(SecretRefHelper.getSecretConfigString(githubUsernameToken.getTokenRef()))
            .username(githubUsernameToken.getUsername())
            .usernameRef(usernameReference)
            .build();
      default:
        throw new UnknownEnumTypeException("Github Http Auth Type", type == null ? null : type.getDisplayName());
    }
  }
  private static String getStringSecretForNullableSecret(SecretRefData secretRefData) {
    String usernameRef = null;
    if (secretRefData != null) {
      usernameRef = SecretRefHelper.getSecretConfigString(secretRefData);
    }
    return usernameRef;
  }

  private GithubApiAccess getApiAcessByType(GithubApiAccessSpecDTO spec, GithubApiAccessType apiAccessType) {
    switch (apiAccessType) {
      case TOKEN:
        final GithubTokenSpecDTO tokenSpec = (GithubTokenSpecDTO) spec;
        return GithubTokenApiAccess.builder()
            .tokenRef(SecretRefHelper.getSecretConfigString(tokenSpec.getTokenRef()))
            .build();
      case GITHUB_APP:
        final GithubAppSpecDTO githubAppSpecDTO = (GithubAppSpecDTO) spec;
        return GithubAppApiAccess.builder()
            .applicationId(githubAppSpecDTO.getApplicationId())
            .installationId(githubAppSpecDTO.getInstallationId())
            .privateKeyRef(SecretRefHelper.getSecretConfigString(githubAppSpecDTO.getPrivateKeyRef()))
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
