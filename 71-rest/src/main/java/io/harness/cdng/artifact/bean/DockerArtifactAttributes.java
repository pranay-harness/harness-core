package io.harness.cdng.artifact.bean;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactAttributes implements ArtifactAttributes {
  /** Docker hub registry connector uuid. */
  String dockerHubConnectorId;
  /** Images in repos need to be referenced via a path */
  String imagePath;
  /** Tag refers to exact tag number */
  String tag;

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