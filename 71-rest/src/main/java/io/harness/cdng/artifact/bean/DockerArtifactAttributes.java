package io.harness.cdng.artifact.bean;

import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import software.wings.common.ComparatorUtils;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactAttributes implements ArtifactAttributes {
  /** Docker hub registry connector identifier. */
  String dockerHubConnector;
  /** Images in repos need to be referenced via a path */
  String imagePath;
  /** Tag refers to exact tag number */
  String tag;

  @Override
  public ArtifactOutcome getArtifactOutcome(ArtifactConfigWrapper artifactConfig) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) artifactConfig;
    return DockerArtifactOutcome.builder()
        .dockerhubConnector(getDockerHubConnector())
        .imagePath(getImagePath())
        .tag(getTag())
        .tagRegex(dockerHubArtifactConfig.getTagRegex())
        .identifier(artifactConfig.getIdentifier())
        .artifactType(artifactConfig.getArtifactType())
        .build();
  }

  @Override
  public int compareTo(@NotNull ArtifactAttributes that) {
    if (that instanceof DockerArtifactAttributes) {
      DockerArtifactAttributes artifactAttributes = (DockerArtifactAttributes) that;
      return ComparatorUtils.compareDecending(this.getTag(), artifactAttributes.getTag());
    } else {
      throw new IllegalArgumentException("ArtifactAttributes list doesn't have all DockerArtifactAttributes elements.");
    }
  }
}