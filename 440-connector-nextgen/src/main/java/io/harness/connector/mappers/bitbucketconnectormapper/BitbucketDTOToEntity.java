package io.harness.connector.mappers.bitbucketconnectormapper;

import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuth;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
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
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class BitbucketDTOToEntity implements ConnectorDTOToEntityMapper<BitbucketConnectorDTO, BitbucketConnector> {
  private SecretRefService secretRefService;

  @Override
  public BitbucketConnector toConnectorEntity(BitbucketConnectorDTO configDTO, NGAccess ngAccess) {
    GitAuthType gitAuthType = getAuthType(configDTO.getAuthentication());
    BitbucketAuthentication bitbucketAuthentication =
        buildAuthenticationDetails(configDTO.getAuthentication().getCredentials(), gitAuthType, ngAccess);
    boolean hasApiAccess = hasApiAccess(configDTO.getApiAccess());
    BitbucketApiAccessType apiAccessType = null;
    BitbucketUsernamePasswordApiAccess bitbucketApiAccess = null;
    if (hasApiAccess) {
      apiAccessType = getApiAccessType(configDTO.getApiAccess());
      bitbucketApiAccess = getApiAccessByType(configDTO.getApiAccess().getSpec(), apiAccessType, ngAccess);
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
      BitbucketCredentialsDTO credentialsDTO, GitAuthType gitAuthType, NGAccess ngAccess) {
    switch (gitAuthType) {
      case SSH:
        final BitbucketSshCredentialsDTO sshCredentialsDTO = (BitbucketSshCredentialsDTO) credentialsDTO;
        return BitbucketSshAuthentication.builder()
            .sshKeyRef(secretRefService.validateAndGetSecretConfigString(sshCredentialsDTO.getSshKeyRef(), ngAccess))
            .build();
      case HTTP:
        final BitbucketHttpCredentialsDTO httpCredentialsDTO = (BitbucketHttpCredentialsDTO) credentialsDTO;
        final BitbucketHttpAuthenticationType type = httpCredentialsDTO.getType();
        return BitbucketHttpAuthentication.builder()
            .type(type)
            .auth(getHttpAuth(type, httpCredentialsDTO, ngAccess))
            .build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Auth Type", String.valueOf(gitAuthType.getDisplayName()));
    }
  }

  private BitbucketHttpAuth getHttpAuth(
      BitbucketHttpAuthenticationType type, BitbucketHttpCredentialsDTO httpCredentialsDTO, NGAccess ngAccess) {
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final BitbucketUsernamePasswordDTO usernamePasswordDTO =
            (BitbucketUsernamePasswordDTO) httpCredentialsDTO.getHttpCredentialsSpec();
        String usernameRef =
            secretRefService.validateAndGetSecretConfigString(usernamePasswordDTO.getUsernameRef(), ngAccess);
        return BitbucketUsernamePassword.builder()
            .passwordRef(
                secretRefService.validateAndGetSecretConfigString(usernamePasswordDTO.getPasswordRef(), ngAccess))
            .username(usernamePasswordDTO.getUsername())
            .usernameRef(usernameRef)
            .build();
      default:
        throw new UnknownEnumTypeException("Bitbucket Http Auth Type", String.valueOf(type.getDisplayName()));
    }
  }

  private BitbucketUsernamePasswordApiAccess getApiAccessByType(
      BitbucketApiAccessSpecDTO spec, BitbucketApiAccessType apiAccessType, NGAccess ngAccess) {
    final BitbucketUsernameTokenApiAccessDTO apiAccessDTO = (BitbucketUsernameTokenApiAccessDTO) spec;
    return BitbucketUsernamePasswordApiAccess.builder()
        .username(apiAccessDTO.getUsername())
        .usernameRef(secretRefService.validateAndGetSecretConfigString(apiAccessDTO.getUsernameRef(), ngAccess))
        .tokenRef(secretRefService.validateAndGetSecretConfigString(apiAccessDTO.getTokenRef(), ngAccess))
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
}
