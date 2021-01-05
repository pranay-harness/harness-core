package io.harness.connector.mappers.bitbucketconnectormapper;

import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuth;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccessDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.UnknownEnumTypeException;

import java.util.Collections;
import java.util.List;

public class BitbucketDTOToEntity implements ConnectorDTOToEntityMapper<BitbucketConnectorDTO> {
  @Override
  public BitbucketConnector toConnectorEntity(BitbucketConnectorDTO configDTO) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    BitbucketAuthentication bitbucketAuthentication =
        buildAuthenticationDetails(configDTO.getAuthentication().getCredentials(), gitAuthType);
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    BitbucketApiAccessType apiAccessType = null;
    BitbucketUsernamePasswordApiAccess bitbucketApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      bitbucketApiAccess = getApiAccessByType(configDTO.getApiAccess().getSpec(), apiAccessType);
    }
    return BitbucketConnector.builder()
        .connectionType(configDTO.getConnectionType())
        .authType(gitAuthType)
        .hasApiAccess(hasApiAccess)
        .authenticationDetails(bitbucketAuthentication)
        .bitbucketApiAccess(bitbucketApiAccess)
        .url(configDTO.getUrl())
        .build();
  }

  private BitbucketAuthentication buildAuthenticationDetails(
      BitbucketCredentialsDTO credentialsDTO, GitAuthType gitAuthType) {
    switch (gitAuthType) {
      case SSH:
        final BitbucketSshCredentialsDTO sshCredentialsDTO = (BitbucketSshCredentialsDTO) credentialsDTO;
        return BitbucketSshAuthentication.builder()
            .sshKeyRef(SecretRefHelper.getSecretConfigString(sshCredentialsDTO.getSpec().getSshKeyRef()))
            .build();
      case HTTP:
        final BitbucketHttpCredentialsDTO httpCredentialsDTO = (BitbucketHttpCredentialsDTO) credentialsDTO;
        final BitbucketHttpAuthenticationType type = httpCredentialsDTO.getType();
        return BitbucketHttpAuthentication.builder().type(type).auth(getHttpAuth(type, httpCredentialsDTO)).build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Auth Type", String.valueOf(gitAuthType.getDisplayName()));
    }
  }

  private BitbucketHttpAuth getHttpAuth(
      BitbucketHttpAuthenticationType type, BitbucketHttpCredentialsDTO httpCredentialsDTO) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final BitbucketUsernamePasswordDTO usernamePasswordDTO =
            (BitbucketUsernamePasswordDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef = getStringSecretForNullableSecret(usernamePasswordDTO.getUsernameRef());
        return BitbucketUsernamePassword.builder()
            .passwordRef(SecretRefHelper.getSecretConfigString(usernamePasswordDTO.getPasswordRef()))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Http Auth Type", String.valueOf(type.getDisplayName()));
    }
  }
  private String getStringSecretForNullableSecret(SecretRefData secretRefData) {
    return SecretRefHelper.getSecretConfigString(secretRefData);
  }

  private BitbucketUsernamePasswordApiAccess getApiAccessByType(
      BitbucketApiAccessSpecDTO spec, BitbucketApiAccessType apiAccessType) {
    final BitbucketUsernameTokenApiAccessDTO apiAccessDTO = (BitbucketUsernameTokenApiAccessDTO) spec;
    return BitbucketUsernamePasswordApiAccess.builder()
        .username(apiAccessDTO.getUsername())
        .usernameRef(getStringSecretForNullableSecret(apiAccessDTO.getUsernameRef()))
        .tokenRef(getStringSecretForNullableSecret(apiAccessDTO.getTokenRef()))
        .build();
  }

  private BitbucketApiAccessType getApiAccessType(BitbucketApiAccessDTO apiAccess) {
    return apiAccess.getType();
  }

  private boolean hasApiAccess(BitbucketApiAccessDTO apiAccess) {
    return apiAccess != null;
  }

  private GitAuthType getAuthType(BitbucketAuthenticationDTO authentication) {
    return authentication.getAuthType();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.CODE_REPO);
  }
}
