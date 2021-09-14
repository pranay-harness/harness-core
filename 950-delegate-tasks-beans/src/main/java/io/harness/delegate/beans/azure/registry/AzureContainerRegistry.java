/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.azure.registry;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;

import java.util.Map;
import java.util.Optional;

public class AzureContainerRegistry extends AzureRegistry {
  @Override
  public Optional<DecryptableEntity> getAuthCredentialsDTO(ConnectorConfigDTO connectorConfigDTO) {
    return Optional.empty();
  }

  @Override
  public Map<String, AzureAppServiceApplicationSetting> getContainerSettings(ConnectorConfigDTO connectorConfigDTO) {
    AzureContainerRegistryConnectorDTO acrConnectorDTO = (AzureContainerRegistryConnectorDTO) connectorConfigDTO;
    String azureRegistryLoginServer = acrConnectorDTO.getAzureRegistryLoginServer();
    validatePublicRegistrySettings(azureRegistryLoginServer);
    return populateDockerSettingMap(azureRegistryLoginServer);
  }
}
