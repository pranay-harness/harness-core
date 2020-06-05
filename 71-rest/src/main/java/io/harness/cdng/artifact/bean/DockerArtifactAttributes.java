package io.harness.cdng.artifact.bean;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactAttributes extends DockerArtifact implements ArtifactAttributes {
  @Override
  public Artifact getArtifact(String accountId, String artifactSourceId, String sourceType) {
    return DockerArtifact.builder()
        .accountId(accountId)
        .dockerHubConnectorId(getDockerHubConnectorId())
        .imagePath(getImagePath())
        .tag(getTag())
        .artifactSourceId(artifactSourceId)
        .sourceType(sourceType)
        .build();
  }
}