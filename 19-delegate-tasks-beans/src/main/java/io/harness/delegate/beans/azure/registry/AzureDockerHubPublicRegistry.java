package io.harness.delegate.beans.azure.registry;

import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;

import java.util.Map;
import java.util.Optional;

public class AzureDockerHubPublicRegistry extends AzureRegistry {
  @Override
  public Optional<DecryptableEntity> getAuthCredentialsDTO(ConnectorConfigDTO connectorConfigDTO) {
    return Optional.empty();
  }

  @Override
  public Map<String, AzureAppServiceDockerSetting> getContainerSettings(ConnectorConfigDTO connectorConfigDTO) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorConfigDTO;
    String dockerRegistryUrl = dockerConnectorDTO.getDockerRegistryUrl();
    validatePublicRegistrySettings(dockerRegistryUrl);
    return populateDockerSettingMap(dockerRegistryUrl);
  }
}
