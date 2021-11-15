package io.harness.connector.mappers.bitbucketconnectormapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuth;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccess;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpec;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthentication;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

@OwnedBy(HarnessTeam.DX)
public class BitbucketDTOToEntity implements ConnectorDTOToEntityMapper<BitbucketConnector,
    io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector> {
  @Override
  public io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector toConnectorEntity(
      BitbucketConnector configDTO) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication bitbucketAuthentication =
        buildAuthenticationDetails(gitAuthType, configDTO.getAuthentication().getCredentials());
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    BitbucketApiAccessType apiAccessType = null;
    BitbucketUsernamePasswordApiAccess bitbucketApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      bitbucketApiAccess = getApiAccessByType(configDTO.getApiAccess().getSpec(), apiAccessType);
    }
    return io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .authenticationDetails(bitbucketAuthentication)
        .bitbucketApiAccess(bitbucketApiAccess)
        .url(configDTO.getUrl())
        .validationRepo(configDTO.getValidationRepo())
        .build();
  }

  public static io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication
  buildAuthenticationDetails(GitAuthType gitAuthType, BitbucketCredentials credentialsDTO) {
    switch (gitAuthType) {
      case SSH:
        final BitbucketSshCredentials sshCredentialsDTO = (BitbucketSshCredentials) credentialsDTO;
        return BitbucketSshAuthentication.builder()
            .sshKeyRef(SecretRefHelper.getSecretConfigString(sshCredentialsDTO.getSshKeyRef()))
            .build();
      case HTTP:
        final BitbucketHttpCredentials httpCredentialsDTO = (BitbucketHttpCredentials) credentialsDTO;
        final BitbucketHttpAuthenticationType type = httpCredentialsDTO.getType();
        return BitbucketHttpAuthentication.builder().type(type).auth(getHttpAuth(type, httpCredentialsDTO)).build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Auth Type", String.valueOf(gitAuthType.getDisplayName()));
    }
  }

  private static BitbucketHttpAuth getHttpAuth(
      BitbucketHttpAuthenticationType type, BitbucketHttpCredentials httpCredentialsDTO) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final BitbucketUsernamePassword usernamePasswordDTO =
            (BitbucketUsernamePassword) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef());
        return io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword.builder()
            .passwordRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getPasswordRef()))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Http Auth Type", String.valueOf(type.getDisplayName()));
    }
  }
  private static String getStringSecretForNullableSecret(SecretRefData secretRefData) {
    return SecretRefHelper.getSecretConfigString(secretRefData);
  }

  private BitbucketUsernamePasswordApiAccess getApiAccessByType(
      BitbucketApiAccessSpec spec, BitbucketApiAccessType apiAccessType) {
    final BitbucketUsernameTokenApiAccess apiAccessDTO = (BitbucketUsernameTokenApiAccess) spec;
    return BitbucketUsernamePasswordApiAccess.builder()
        .username(apiAccessDTO.getUsername())
        .usernameRef(getStringSecretForNullableSecret(apiAccessDTO.getUsernameRef()))
        .tokenRef(getStringSecretForNullableSecret(apiAccessDTO.getTokenRef()))
        .build();
  }

  private BitbucketApiAccessType getApiAccessType(BitbucketApiAccess apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(BitbucketApiAccess apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(BitbucketAuthentication authentication) {
    return authentication.getAuthType();
  }
}
