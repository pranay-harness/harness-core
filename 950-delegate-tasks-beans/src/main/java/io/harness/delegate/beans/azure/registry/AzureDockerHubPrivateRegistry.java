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
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;

import java.util.Map;
import java.util.Optional;

public class AzureDockerHubPrivateRegistry extends AzureRegistry {
  @Override
  public Optional<DecryptableEntity> getAuthCredentialsDTO(ConnectorConfigDTO connectorConfigDTO) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorConfigDTO;
    return dockerConnectorDTO.getAuth() != null
        ? Optional.ofNullable((DockerUserNamePasswordDTO) dockerConnectorDTO.getAuth().getCredentials())
        : Optional.empty();
  }

  @Override
  public Map<String, AzureAppServiceApplicationSetting> getContainerSettings(ConnectorConfigDTO connectorConfigDTO) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorConfigDTO;
    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        (DockerUserNamePasswordDTO) dockerConnectorDTO.getAuth().getCredentials();

    String dockerRegistryUrl = dockerConnectorDTO.getDockerRegistryUrl();
    String decryptedSecret = new String(dockerUserNamePasswordDTO.getPasswordRef().getDecryptedValue());
    String username = dockerUserNamePasswordDTO.getUsername();
    validatePrivateRegistrySettings(dockerRegistryUrl, username, decryptedSecret);
    return populateDockerSettingMap(dockerRegistryUrl, username, decryptedSecret);
  }
}
