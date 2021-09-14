/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.azurekeyvaultconnector.AzureKeyVaultConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(PL)
public class AzureKeyVaultEntityToDTO
    implements ConnectorEntityToDTOMapper<AzureKeyVaultConnectorDTO, AzureKeyVaultConnector> {
  @Override
  public AzureKeyVaultConnectorDTO createConnectorDTO(AzureKeyVaultConnector connector) {
    return AzureKeyVaultConnectorDTO.builder()
        .isDefault(connector.isDefault())
        .clientId(connector.getClientId())
        .tenantId(connector.getTenantId())
        .vaultName(connector.getVaultName())
        .secretKey(SecretRefHelper.createSecretRef(connector.getSecretKeyRef()))
        .subscription(connector.getSubscription())
        .azureEnvironmentType(connector.getAzureEnvironmentType())
        .delegateSelectors(connector.getDelegateSelectors())
        .build();
  }
}
