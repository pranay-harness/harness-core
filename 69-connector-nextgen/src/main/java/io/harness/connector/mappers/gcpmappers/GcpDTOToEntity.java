package io.harness.connector.mappers.gcpmappers;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpSecretKeyAuth;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.gcpconnector.GcpAuthDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpAuthType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpSecretKeyAuthDTO;
import io.harness.exception.InvalidRequestException;

@Singleton
public class GcpDTOToEntity implements ConnectorDTOToEntityMapper<GcpConnectorDTO> {
  @Override
  public GcpConfig toConnectorEntity(GcpConnectorDTO connectorDTO) {
    final GcpCredentialType credentialType = connectorDTO.getGcpCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(connectorDTO);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(connectorDTO);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private GcpConfig buildManualCredential(GcpConnectorDTO connector) {
    final GcpDetailsDTO connectorConfig = (GcpDetailsDTO) connector.getConfig();
    final GcpAuthType authType = connectorConfig.getAuth().getAuthType();
    GcpDetails gcpAuth = null;
    switch (authType) {
      case SECRET_KEY:
        gcpAuth = buildSecretKeyAuth(connectorConfig.getAuth());
        break;
      default:
        throw new InvalidRequestException("Invalid Auth type");
    }
    return GcpConfig.builder().credentialType(GcpCredentialType.MANUAL_CREDENTIALS).credential(gcpAuth).build();
  }

  private GcpDetails buildSecretKeyAuth(GcpAuthDTO gcpAuthDTO) {
    final GcpSecretKeyAuthDTO credentials = (GcpSecretKeyAuthDTO) gcpAuthDTO.getCredentials();
    final String secretConfigString = SecretRefHelper.getSecretConfigString(credentials.getSecretKeyRef());
    GcpSecretKeyAuth gcpSecretKeyAuth = GcpSecretKeyAuth.builder().secretKeyRef(secretConfigString).build();
    return GcpDetails.builder().auth(gcpSecretKeyAuth).authType(GcpAuthType.SECRET_KEY).build();
  }

  private GcpConfig buildInheritFromDelegate(GcpConnectorDTO connector) {
    final GcpDelegateDetailsDTO gcpCredential = (GcpDelegateDetailsDTO) connector.getConfig();
    GcpDelegateDetails gcpDelegateDetails =
        GcpDelegateDetails.builder().delegateSelector(gcpCredential.getDelegateSelector()).build();
    return GcpConfig.builder()
        .credentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
        .credential(gcpDelegateDetails)
        .build();
  }
}