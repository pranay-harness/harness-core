/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(PL)
public class GcpKmsEntityToDTO implements ConnectorEntityToDTOMapper<GcpKmsConnectorDTO, GcpKmsConnector> {
  @Override
  public GcpKmsConnectorDTO createConnectorDTO(GcpKmsConnector connector) {
    GcpKmsConnectorDTO gcpKmsConnectorDTO =
        GcpKmsConnectorDTO.builder()
            .keyName(connector.getKeyName())
            .keyRing(connector.getKeyRing())
            .projectId(connector.getProjectId())
            .region(connector.getRegion())
            .isDefault(connector.isDefault())
            .credentials(SecretRefHelper.createSecretRef(connector.getCredentialsRef()))
            .delegateSelectors(connector.getDelegateSelectors())
            .build();
    gcpKmsConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));
    return gcpKmsConnectorDTO;
  }
}
