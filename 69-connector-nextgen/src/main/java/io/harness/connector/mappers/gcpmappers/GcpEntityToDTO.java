package io.harness.connector.mappers.gcpmappers;

import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpDetails;
import io.harness.connector.entities.embedded.gcpconnector.GcpSecretKeyAuth;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.gcpconnector.GcpAuthDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpAuthType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpSecretKeyAuthDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

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
    final GcpDetails gcpDetails = (GcpDetails) connector.getCredential();
    final GcpAuthType authType = gcpDetails.getAuthType();
    GcpDetailsDTO gcpDetailsDTO = null;
    switch (authType) {
      case SECRET_KEY:
        gcpDetailsDTO = buildSecretKeyAuth(gcpDetails);
        break;
      default:
        throw new InvalidRequestException("Invalid Auth type");
    }
    return GcpConnectorDTO.builder()
        .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
        .config(gcpDetailsDTO)
        .build();
  }

  private GcpDetailsDTO buildSecretKeyAuth(GcpDetails gcpDetails) {
    final GcpSecretKeyAuth auth = (GcpSecretKeyAuth) gcpDetails.getAuth();
    final SecretRefData secretRef = SecretRefHelper.createSecretRef(auth.getSecretKeyRef());
    final GcpSecretKeyAuthDTO secretKeyAuth = GcpSecretKeyAuthDTO.builder().secretKeyRef(secretRef).build();
    final GcpAuthDTO authDTO = GcpAuthDTO.builder().authType(GcpAuthType.SECRET_KEY).credentials(secretKeyAuth).build();
    return GcpDetailsDTO.builder().auth(authDTO).build();
  }

  private GcpConnectorDTO buildInheritFromDelegate(GcpConfig connector) {
    final GcpDelegateDetails gcpCredential = (GcpDelegateDetails) connector.getCredential();
    GcpDelegateDetailsDTO gcpDelegateDetailsDTO =
        GcpDelegateDetailsDTO.builder().delegateSelector(gcpCredential.getDelegateSelector()).build();
    return GcpConnectorDTO.builder()
        .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
        .config(gcpDelegateDetailsDTO)
        .build();
  }
}