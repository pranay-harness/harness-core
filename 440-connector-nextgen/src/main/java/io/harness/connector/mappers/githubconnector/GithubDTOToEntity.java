package io.harness.connector.mappers.githubconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuth;
import io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
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
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentials;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpec;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePassword;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameToken;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

@OwnedBy(HarnessTeam.DX)
public class GithubDTOToEntity implements ConnectorDTOToEntityMapper<GithubConnector,
    io.harness.connector.entities.embedded.githubconnector.GithubConnector> {
  @Override
  public io.harness.connector.entities.embedded.githubconnector.GithubConnector toConnectorEntity(
      GithubConnector configDTO) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    io.harness.connector.entities.embedded.githubconnector.GithubAuthentication githubAuthentication =
        buildAuthenticationDetails(gitAuthType, configDTO.getAuthentication().getCredentials());
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    GithubApiAccessType apiAccessType = null;
    io.harness.connector.entities.embedded.githubconnector.GithubApiAccess githubApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      githubApiAccess = getApiAcessByType(configDTO.getApiAccess().getSpec(), apiAccessType);
    }
    return io.harness.connector.entities.embedded.githubconnector.GithubConnector.builder()
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

  public static io.harness.connector.entities.embedded.githubconnector.GithubAuthentication buildAuthenticationDetails(
      GitAuthType gitAuthType, GithubCredentials credentialsDTO) {
    switch (gitAuthType) {
      case SSH:
        final GithubSshCredentials sshCredentialsDTO = (GithubSshCredentials) credentialsDTO;
        return GithubSshAuthentication.builder()
            .sshKeyRef(SecretRefHelper.getSecretConfigString(sshCredentialsDTO.getSshKeyRef()))
            .build();
      case HTTP:
        final GithubHttpCredentials httpCredentialsDTO = (GithubHttpCredentials) credentialsDTO;
        final GithubHttpAuthenticationType type = httpCredentialsDTO.getType();
        return GithubHttpAuthentication.builder().type(type).auth(getHttpAuth(type, httpCredentialsDTO)).build();
      default:
        throw new UnknownEnumTypeException(
            "Github Auth Type", gitAuthType == null ? null : gitAuthType.getDisplayName());
    }
  }

  private static GithubHttpAuth getHttpAuth(
      GithubHttpAuthenticationType type, GithubHttpCredentials httpCredentialsDTO) {
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

  private io.harness.connector.entities.embedded.githubconnector.GithubApiAccess getApiAcessByType(
      GithubApiAccessSpec spec, GithubApiAccessType apiAccessType) {
    switch (apiAccessType) {
      case TOKEN:
        final GithubTokenSpec tokenSpec = (GithubTokenSpec) spec;
        return GithubTokenApiAccess.builder()
            .tokenRef(SecretRefHelper.getSecretConfigString(tokenSpec.getTokenRef()))
            .build();
      case GITHUB_APP:
        final GithubAppSpec githubAppSpec = (GithubAppSpec) spec;
        return GithubAppApiAccess.builder()
            .applicationId(githubAppSpec.getApplicationId())
            .installationId(githubAppSpec.getInstallationId())
            .privateKeyRef(SecretRefHelper.getSecretConfigString(githubAppSpec.getPrivateKeyRef()))
            .build();
      default:
        throw new UnknownEnumTypeException(
            "Github Api Access Type", apiAccessType == null ? null : apiAccessType.getDisplayName());
    }
  }

  private GithubApiAccessType getApiAccessType(GithubApiAccess apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(GithubApiAccess apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(GithubAuthentication authentication) {
    return authentication.getAuthType();
  }
}
