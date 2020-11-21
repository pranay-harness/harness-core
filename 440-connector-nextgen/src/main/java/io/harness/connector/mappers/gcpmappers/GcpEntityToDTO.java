package io.harness.connector.mappers.gcpmappers;

import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class GcpEntityToDTO implements ConnectorEntityToDTOMapper<GcpConfig> {
  @Override
  public GcpConnectorDTO createConnectorDTO(GcpConfig connector) {
    final GcpCredentialType credentialType = connector.getCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(connector);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(connector);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private GcpConnectorDTO buildManualCredential(GcpConfig connector) {
    final GcpServiceAccountKey auth = (GcpServiceAccountKey) connector.getCredential();
    final SecretRefData secretRef = SecretRefHelper.createSecretRef(auth.getSecretKeyRef());
    final GcpManualDetailsDTO gcpManualDetailsDTO = GcpManualDetailsDTO.builder().secretKeyRef(secretRef).build();
    return GcpConnectorDTO.builder()
        .credential(GcpConnectorCredentialDTO.builder()
                        .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                        .config(gcpManualDetailsDTO)
                        .build())
        .build();
  }

  private GcpConnectorDTO buildInheritFromDelegate(GcpConfig connector) {
    final GcpDelegateDetails gcpCredential = (GcpDelegateDetails) connector.getCredential();
    GcpDelegateDetailsDTO gcpDelegateDetailsDTO =
        GcpDelegateDetailsDTO.builder().delegateSelector(gcpCredential.getDelegateSelector()).build();
    return GcpConnectorDTO.builder()
        .credential(GcpConnectorCredentialDTO.builder()
                        .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                        .config(gcpDelegateDetailsDTO)
                        .build())
        .build();
  }
}
