package software.wings.sm.states.azure.artifact;

import static io.harness.azure.model.AzureConstants.ARTIFACT_PATH_PREFIX;

import static software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;

import java.util.List;
import java.util.Optional;

public class ArtifactStreamAttributesMapper extends ArtifactConnectorMapper {
  protected ArtifactStreamAttributesMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    super(artifact, artifactStreamAttributes);
    populateArtifactStreamAttributes();
  }

  private void populateArtifactStreamAttributes() {
    artifactStreamAttributes.getMetadata().put(ArtifactMetadataKeys.artifactFileName, artifactFileName());
    artifactStreamAttributes.getMetadata().put(ArtifactMetadataKeys.artifactPath, artifactPath());
  }

  private String artifactFileName() {
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType());
    switch (artifactStreamType) {
      case BAMBOO:
        return getBambooArtifactFileName();
      case ARTIFACTORY:
        return getArtifactoryArtifactFileName();
      case NEXUS:
        return getNexusArtifactFileName();
      default:
        return artifact.getDisplayName();
    }
  }

  private String getBambooArtifactFileName() {
    return getArtifactPath();
  }

  private String getArtifactoryArtifactFileName() {
    return getArtifactBuildNumberFromStreamMetadata();
  }

  private String getNexusArtifactFileName() {
    return artifact.getDisplayName().substring(artifact.getArtifactSourceName().length());
  }

  private String artifactPath() {
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType());
    switch (artifactStreamType) {
      case JENKINS:
        return getJenkinsArtifactPath();
      case BAMBOO:
        return getBambooArtifactPath();
      case ARTIFACTORY:
        return getArtifactoryArtifactPath();
      default:
        return getArtifactUrlFromStreamMetadata();
    }
  }

  private String getJenkinsArtifactPath() {
    return ARTIFACT_PATH_PREFIX + getArtifactPath();
  }

  private String getBambooArtifactPath() {
    List<ArtifactFileMetadata> artifactFileMetadata = artifact.getArtifactFileMetadata();
    if (artifactFileMetadata.isEmpty()) {
      throw new InvalidRequestException("artifact url is required");
    }
    return artifactFileMetadata.get(0).getUrl();
  }

  private String getArtifactoryArtifactPath() {
    String artifactUrl = getArtifactUrlFromStreamMetadata();
    String jobName = artifactStreamAttributes.getJobName();
    return "." + artifactUrl.substring(artifactUrl.lastIndexOf(jobName) + jobName.length());
  }

  public String getArtifactPath() {
    List<String> artifactPaths = artifactStreamAttributes.getArtifactPaths();
    if (artifactPaths.isEmpty()) {
      throw new InvalidRequestException("ArtifactPath is missing!");
    }
    return artifactPaths.get(0);
  }

  private String getArtifactUrlFromStreamMetadata() {
    return artifactStreamAttributes.getMetadata().get("url");
  }

  private String getArtifactBuildNumberFromStreamMetadata() {
    return artifactStreamAttributes.getMetadata().get("buildNo");
  }

  @Override
  public ConnectorConfigDTO getConnectorDTO() {
    return null;
  }

  @Override
  public AzureRegistryType getAzureRegistryType() {
    return null;
  }

  @Override
  public boolean isDockerArtifactType() {
    return false;
  }

  @Override
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    return Optional.empty();
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.ofNullable((EncryptableSetting) artifactStreamAttributes.getServerSetting().getValue());
  }
}
