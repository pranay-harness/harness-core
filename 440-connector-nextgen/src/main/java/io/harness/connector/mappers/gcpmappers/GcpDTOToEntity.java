package io.harness.connector.mappers.gcpmappers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class GcpDTOToEntity implements ConnectorDTOToEntityMapper<GcpConnectorDTO, GcpConfig> {
  @Override
  public GcpConfig toConnectorEntity(GcpConnectorDTO connectorDTO) {
    final GcpConnectorCredentialDTO credential = connectorDTO.getCredential();
    final GcpCredentialType credentialType = credential.getGcpCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(credential);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(credential);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private GcpConfig buildManualCredential(GcpConnectorCredentialDTO connector) {
    final GcpManualDetailsDTO connectorConfig = (GcpManualDetailsDTO) connector.getConfig();
    final String secretConfigString = SecretRefHelper.getSecretConfigString(connectorConfig.getSecretKeyRef());
    GcpServiceAccountKey gcpSecretKeyAuth = GcpServiceAccountKey.builder().secretKeyRef(secretConfigString).build();
    return GcpConfig.builder()
        .credentialType(GcpCredentialType.MANUAL_CREDENTIALS)
        .credential(gcpSecretKeyAuth)
        .build();
  }

  private GcpConfig buildInheritFromDelegate(GcpConnectorCredentialDTO connector) {
    return GcpConfig.builder().credentialType(GcpCredentialType.INHERIT_FROM_DELEGATE).credential(null).build();
  }
}
