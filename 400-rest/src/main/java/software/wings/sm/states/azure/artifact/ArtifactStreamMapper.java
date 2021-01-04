package software.wings.sm.states.azure.artifact;

import static java.lang.String.format;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;

import java.util.Optional;

public abstract class ArtifactStreamMapper {
  protected ArtifactStream artifactStream;

  protected ArtifactStreamMapper(ArtifactStream artifactStream) {
    this.artifactStream = artifactStream;
  }

  public static ArtifactStreamMapper getArtifactStreamMapper(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (ArtifactStreamType.DOCKER.name().equals(artifactStreamType)) {
      return new DockerArtifactStreamMapper(artifactStream);
    } else if (ArtifactStreamType.ARTIFACTORY.name().equals(artifactStreamType)) {
      return new ArtifactoryArtifactStreamMapper(artifactStream);
    } else if (ArtifactStreamType.ACR.name().equals(artifactStreamType)) {
      return new ACRArtifactStreamMapper(artifactStream);
    } else {
      throw new InvalidRequestException(
          format("Unsupported artifact stream type for Azure Web Application deployment type %s", artifactStreamType));
    }
  }

  public abstract ConnectorConfigDTO getConnectorDTO();
  public abstract AzureRegistryType getAzureRegistryType();
  public abstract Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO);
}
