/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
