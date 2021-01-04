package io.harness.connector.mappers.bitbucketconnectormapper;

import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuth;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsSpecDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.UnknownEnumTypeException;

public class BitbucketEntityToDTO implements ConnectorEntityToDTOMapper<BitbucketConnector> {
  @Override
  public BitbucketConnectorDTO createConnectorDTO(BitbucketConnector connector) {
    BitbucketAuthenticationDTO bitbucketAuthenticationDTO = buildBitbucketAuthentication(connector);
    BitbucketApiAccessDTO bitbucketApiAccess = null;
    if (connector.isHasApiAccess()) {
      bitbucketApiAccess = buildApiAccess(connector);
    }
    return BitbucketConnectorDTO.builder()
        .apiAccess(bitbucketApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(bitbucketAuthenticationDTO)
        .url(connector.getUrl())
        .build();
  }

  private BitbucketAuthenticationDTO buildBitbucketAuthentication(BitbucketConnector connector) {
    final GitAuthType authType = connector.getAuthType();
    final BitbucketAuthentication authenticationDetails = connector.getAuthenticationDetails();
    BitbucketCredentialsDTO bitbucketCredentialsDTO = null;
    switch (authType) {
      case SSH:
        final BitbucketSshAuthentication bitbucketSshAuthentication =
            (BitbucketSshAuthentication) authenticationDetails;
        final BitbucketSshCredentialsSpecDTO bitbucketSshCredentialsSpecDTO =
            BitbucketSshCredentialsSpecDTO.builder()
                .sshKeyRef(SecretRefHelper.createSecretRef(bitbucketSshAuthentication.getSshKeyRef()))
                .build();
        bitbucketCredentialsDTO = BitbucketSshCredentialsDTO.builder().spec(bitbucketSshCredentialsSpecDTO).build();
        break;
      case HTTP:
        final BitbucketHttpAuthentication bitbucketHttpAuthentication =
            (BitbucketHttpAuthentication) authenticationDetails;
        final BitbucketHttpAuthenticationType type = bitbucketHttpAuthentication.getType();
        final BitbucketHttpAuth auth = bitbucketHttpAuthentication.getAuth();
        BitbucketHttpCredentialsSpecDTO bitbucketHttpCredentialsSpecDTO = getHttpCredentialsSpecDTO(type, auth);
        bitbucketCredentialsDTO = BitbucketHttpCredentialsDTO.builder()
                                      .type(type)
                                      .httpCredentialsSpec(bitbucketHttpCredentialsSpecDTO)
                                      .build();
        break;
      default:
        throw new UnknownEnumTypeException("Bitbucket Auth Type", String.valueOf(authType.getDisplayName()));
    }
    return BitbucketAuthenticationDTO.builder().authType(authType).credentials(bitbucketCredentialsDTO).build();
  }

  private BitbucketHttpCredentialsSpecDTO getHttpCredentialsSpecDTO(BitbucketHttpAuthenticationType type, Object auth) {
    BitbucketHttpCredentialsSpecDTO bitbucketHttpCredentialsSpecDTO = null;
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final BitbucketUsernamePassword bitbucketUsernamePassword = (BitbucketUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (bitbucketUsernamePassword.getUsernameRef() != null) {
          usernameRef = SecretRefHelper.createSecretRef(bitbucketUsernamePassword.getUsernameRef());
        }
        bitbucketHttpCredentialsSpecDTO =
            BitbucketUsernamePasswordDTO.builder()
                .passwordRef(SecretRefHelper.createSecretRef(bitbucketUsernamePassword.getPasswordRef()))
                .username(bitbucketUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      default:
        throw new UnknownEnumTypeException("Bitbucket Http Auth Type", String.valueOf(type.getDisplayName()));
    }
    return bitbucketHttpCredentialsSpecDTO;
  }

  private BitbucketApiAccessDTO buildApiAccess(BitbucketConnector connector) {
    final BitbucketUsernamePasswordApiAccess bitbucketTokenApiAccess = connector.getBitbucketApiAccess();
    SecretRefData usernameRef = bitbucketTokenApiAccess.getUsernameRef() != null
        ? SecretRefHelper.createSecretRef(bitbucketTokenApiAccess.getUsernameRef())
        : null;
    final BitbucketApiAccessSpecDTO bitbucketTokenSpecDTO =

        BitbucketUsernamePasswordApiAccessDTO.builder()
            .username(bitbucketTokenApiAccess.getUsername())
            .usernameRef(usernameRef)
            .passwordRef(SecretRefHelper.createSecretRef(bitbucketTokenApiAccess.getPasswordRef()))
            .build();
    return BitbucketApiAccessDTO.builder()
        .type(BitbucketApiAccessType.USERNAME_AND_PASSWORD)
        .spec(bitbucketTokenSpecDTO)
        .build();
  }
}
