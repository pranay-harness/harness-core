package io.harness.connector.mappers.gcpmappers;

import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GcpDTOToEntity implements ConnectorDTOToEntityMapper<GcpConnectorDTO, GcpConfig> {
  private SecretRefService secretRefService;

  @Override
  public GcpConfig toConnectorEntity(GcpConnectorDTO connectorDTO, NGAccess ngAccess) {
    final GcpConnectorCredentialDTO credential = connectorDTO.getCredential();
    final GcpCredentialType credentialType = credential.getGcpCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(credential);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(credential, ngAccess);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private GcpConfig buildManualCredential(GcpConnectorCredentialDTO connector, NGAccess ngAccess) {
    final GcpManualDetailsDTO connectorConfig = (GcpManualDetailsDTO) connector.getConfig();
    final String secretConfigString =
        secretRefService.validateAndGetSecretConfigString(connectorConfig.getSecretKeyRef(), ngAccess);
    GcpServiceAccountKey gcpSecretKeyAuth = GcpServiceAccountKey.builder().secretKeyRef(secretConfigString).build();
    return GcpConfig.builder()
        .credentialType(GcpCredentialType.MANUAL_CREDENTIALS)
        .credential(gcpSecretKeyAuth)
        .build();
  }

  private GcpConfig buildInheritFromDelegate(GcpConnectorCredentialDTO connector) {
    final GcpDelegateDetailsDTO gcpCredential = (GcpDelegateDetailsDTO) connector.getConfig();
    GcpDelegateDetails gcpDelegateDetails =
        GcpDelegateDetails.builder().delegateSelector(gcpCredential.getDelegateSelector()).build();
    return GcpConfig.builder()
        .credentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
        .credential(gcpDelegateDetails)
        .build();
  }
}
