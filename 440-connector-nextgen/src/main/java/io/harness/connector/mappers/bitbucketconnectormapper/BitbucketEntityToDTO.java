package io.harness.connector.mappers.bitbucketconnectormapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuth;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccess;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpec;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthentication;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsSpec;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentials;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePassword;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccess;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnknownEnumTypeException;

@OwnedBy(HarnessTeam.DX)
public class BitbucketEntityToDTO implements ConnectorEntityToDTOMapper<BitbucketConnector,
    io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector> {
  @Override
  public BitbucketConnector createConnectorDTO(
      io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector connector) {
    BitbucketAuthentication bitbucketAuthentication =
        buildBitbucketAuthentication(connector.getAuthType(), connector.getAuthenticationDetails());
    BitbucketApiAccess bitbucketApiAccess = null;
    if (connector.isHasApiAccess()) {
      bitbucketApiAccess = buildApiAccess(connector);
    }
    return BitbucketConnector.builder()
        .apiAccess(bitbucketApiAccess)
        .connectionType(connector.getConnectionType())
        .authentication(bitbucketAuthentication)
        .url(connector.getUrl())
        .validationRepo(connector.getValidationRepo())
        .build();
  }

  public static BitbucketAuthentication buildBitbucketAuthentication(GitAuthType authType,
      io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication authenticationDetails) {
    BitbucketCredentials bitbucketCredentialsDTO = null;
    switch (authType) {
      case SSH:
        final BitbucketSshAuthentication bitbucketSshAuthentication =
            (BitbucketSshAuthentication) authenticationDetails;
        bitbucketCredentialsDTO =
            BitbucketSshCredentials.builder()
                .sshKeyRef(SecretRefHelper.createSecretRef(bitbucketSshAuthentication.getSshKeyRef()))
                .build();
        break;
      case HTTP:
        final BitbucketHttpAuthentication bitbucketHttpAuthentication =
            (BitbucketHttpAuthentication) authenticationDetails;
        final BitbucketHttpAuthenticationType type = bitbucketHttpAuthentication.getType();
        final BitbucketHttpAuth auth = bitbucketHttpAuthentication.getAuth();
        BitbucketHttpCredentialsSpec bitbucketHttpCredentialsSpec = getHttpCredentialsSpecDTO(type, auth);
        bitbucketCredentialsDTO =
            BitbucketHttpCredentials.builder().type(type).httpCredentialsSpec(bitbucketHttpCredentialsSpec).build();
        break;
      default:
        throw new UnknownEnumTypeException("Bitbucket Auth Type", String.valueOf(authType.getDisplayName()));
    }
    return BitbucketAuthentication.builder().authType(authType).credentials(bitbucketCredentialsDTO).build();
  }

  private static BitbucketHttpCredentialsSpec getHttpCredentialsSpecDTO(
      BitbucketHttpAuthenticationType type, Object auth) {
    BitbucketHttpCredentialsSpec bitbucketHttpCredentialsSpec = null;
    switch (type) {
      case USERNAME_AND_PASSWORD:
        final io.harness.connector.entities.embedded.bitbucketconnector
            .BitbucketUsernamePassword bitbucketUsernamePassword =
            (io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword) auth;
        SecretRefData usernameRef = null;
        if (bitbucketUsernamePassword.getUsernameRef() != null) {
          usernameRef = SecretRefHelper.createSecretRef(bitbucketUsernamePassword.getUsernameRef());
        }
        bitbucketHttpCredentialsSpec =
            BitbucketUsernamePassword.builder()
                .passwordRef(SecretRefHelper.createSecretRef(bitbucketUsernamePassword.getPasswordRef()))
                .username(bitbucketUsernamePassword.getUsername())
                .usernameRef(usernameRef)
                .build();
        break;
      default:
        throw new UnknownEnumTypeException("Bitbucket Http Auth Type", String.valueOf(type.getDisplayName()));
    }
    return bitbucketHttpCredentialsSpec;
  }

  private BitbucketApiAccess buildApiAccess(
      io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector connector) {
    final BitbucketUsernamePasswordApiAccess bitbucketTokenApiAccess = connector.getBitbucketApiAccess();
    SecretRefData usernameRef = bitbucketTokenApiAccess.getUsernameRef() != null
        ? SecretRefHelper.createSecretRef(bitbucketTokenApiAccess.getUsernameRef())
        : null;
    final BitbucketApiAccessSpec bitbucketTokenSpecDTO =

        BitbucketUsernameTokenApiAccess.builder()
            .username(bitbucketTokenApiAccess.getUsername())
            .usernameRef(usernameRef)
            .tokenRef(SecretRefHelper.createSecretRef(bitbucketTokenApiAccess.getTokenRef()))
            .build();
    return BitbucketApiAccess.builder()
        .type(BitbucketApiAccessType.USERNAME_AND_TOKEN)
        .spec(bitbucketTokenSpecDTO)
        .build();
  }
}
