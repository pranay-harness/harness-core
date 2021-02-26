package software.wings.sm.states.azure.artifact;

import static java.lang.String.format;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;

import java.util.Optional;

public abstract class ArtifactStreamMapper {
  protected ArtifactStreamAttributes artifactStreamAttributes;
  protected Artifact artifact;

  protected ArtifactStreamMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    this.artifactStreamAttributes = artifactStreamAttributes;
    this.artifact = artifact;
  }

  public static ArtifactStreamMapper getArtifactStreamMapper(
      Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();

    if (ArtifactStreamType.DOCKER.name().equals(artifactStreamType)) {
      return new DockerArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else if (ArtifactStreamType.ARTIFACTORY.name().equals(artifactStreamType)) {
      return new ArtifactoryArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else if (ArtifactStreamType.ACR.name().equals(artifactStreamType)) {
      return new ACRArtifactStreamMapper(artifact, artifactStreamAttributes);
    } else {
      throw new InvalidRequestException(
          format("Unsupported artifact stream type for Azure Web Application deployment type %s", artifactStreamType));
    }
  }

  public abstract ConnectorConfigDTO getConnectorDTO();
  public abstract AzureRegistryType getAzureRegistryType();
  public abstract Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO);
  public abstract Optional<EncryptableSetting> getEncryptableSetting();

  public String getFullImageName() {
    return artifact.getMetadata().get("image");
  }

  public String getImageTag() {
    return artifact.getMetadata().get("tag");
  }
}
