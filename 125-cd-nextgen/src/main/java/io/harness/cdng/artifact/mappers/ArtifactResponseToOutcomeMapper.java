package io.harness.cdng.artifact.mappers;

import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactResponseToOutcomeMapper {
  public ArtifactOutcome toArtifactOutcome(
      ArtifactConfig artifactConfig, ArtifactDelegateResponse artifactDelegateResponse) {
    switch (artifactConfig.getSourceType()) {
      case DOCKER_HUB:
        DockerHubArtifactConfig dockerConfig = (DockerHubArtifactConfig) artifactConfig;
        DockerArtifactDelegateResponse dockerDelegateResponse =
            (DockerArtifactDelegateResponse) artifactDelegateResponse;
        return getDockerArtifactOutcome(dockerConfig, dockerDelegateResponse);
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", artifactConfig.getSourceType()));
    }
  }

  private DockerArtifactOutcome getDockerArtifactOutcome(
      DockerHubArtifactConfig dockerConfig, DockerArtifactDelegateResponse dockerDelegateResponse) {
    return DockerArtifactOutcome.builder()
        .dockerhubConnector(dockerConfig.getDockerhubConnector().getValue())
        .imagePath(dockerConfig.getImagePath().getValue())
        .tag(dockerDelegateResponse.getTag())
        .tagRegex(dockerConfig.getTagRegex() != null ? dockerConfig.getTagRegex().getValue() : null)
        .identifier(dockerConfig.getIdentifier())
        .primaryArtifact(dockerConfig.isPrimaryArtifact())
        .build();
  }
}
