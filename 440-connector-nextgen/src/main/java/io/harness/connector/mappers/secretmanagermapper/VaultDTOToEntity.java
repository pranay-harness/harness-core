/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(PL)
public class VaultDTOToEntity implements ConnectorDTOToEntityMapper<VaultConnectorDTO, VaultConnector> {
  @Override
  public VaultConnector toConnectorEntity(VaultConnectorDTO connectorDTO) {
    return VaultConnector.builder()
        .authTokenRef(SecretRefHelper.getSecretConfigString(connectorDTO.getAuthToken()))
        .accessType(connectorDTO.getAccessType())
        .isDefault(connectorDTO.isDefault())
        .isReadOnly(connectorDTO.isReadOnly())
        .secretEngineName(connectorDTO.getSecretEngineName())
        .vaultUrl(connectorDTO.getVaultUrl())
        .secretEngineVersion(connectorDTO.getSecretEngineVersion())
        .secretEngineManuallyConfigured(connectorDTO.isSecretEngineManuallyConfigured())
        .renewalIntervalMinutes(connectorDTO.getRenewalIntervalMinutes())
        .renewedAt(System.currentTimeMillis())
        .appRoleId(connectorDTO.getAppRoleId())
        .basePath(connectorDTO.getBasePath())
        .namespace(connectorDTO.getNamespace())
        .sinkPath(connectorDTO.getSinkPath())
        .useVaultAgent(connectorDTO.isUseVaultAgent())
        .secretIdRef(SecretRefHelper.getSecretConfigString(connectorDTO.getSecretId()))
        .build();
  }
}
