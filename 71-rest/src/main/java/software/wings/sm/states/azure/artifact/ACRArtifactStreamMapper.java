package software.wings.sm.states.azure.artifact;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.util.Optional;

public class ACRArtifactStreamMapper extends ArtifactStreamMapper {
  protected ACRArtifactStreamMapper(ArtifactStream artifactStream) {
    super(artifactStream);
  }

  @Override
  public ConnectorConfigDTO getConnectorDTO() {
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    String registryName = artifactStreamAttributes.getRegistryName();
    String registryHostName = artifactStreamAttributes.getRegistryHostName();
    String azureResourceGroup = artifactStreamAttributes.getAzureResourceGroup();
    String subscriptionId = artifactStreamAttributes.getSubscriptionId();

    return AzureContainerRegistryConnectorDTO.builder()
        .azureRegistryLoginServer(registryHostName)
        .azureRegistryName(registryName)
        .resourceGroupName(azureResourceGroup)
        .subscriptionId(subscriptionId)
        .build();
  }

  public AzureRegistryType getAzureRegistryType() {
    return AzureRegistryType.ACR;
  }

  @Override
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    return Optional.empty();
  }
}
