/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.gcpmappers;

import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@Singleton
public class GcpEntityToDTO implements ConnectorEntityToDTOMapper<GcpConnectorDTO, GcpConfig> {
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
        .delegateSelectors(connector.getDelegateSelectors())
        .credential(GcpConnectorCredentialDTO.builder()
                        .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                        .config(gcpManualDetailsDTO)
                        .build())
        .build();
  }

  private GcpConnectorDTO buildInheritFromDelegate(GcpConfig connector) {
    return GcpConnectorDTO.builder()
        .delegateSelectors(connector.getDelegateSelectors())
        .credential(GcpConnectorCredentialDTO.builder()
                        .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                        .config(null)
                        .build())
        .build();
  }
}
