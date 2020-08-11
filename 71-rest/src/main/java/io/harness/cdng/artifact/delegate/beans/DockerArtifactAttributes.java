package io.harness.cdng.artifact.delegate.beans;

import io.harness.artifact.ComparatorUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

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
  public int compareTo(@NotNull ArtifactAttributes that) {
    if (that instanceof DockerArtifactAttributes) {
      DockerArtifactAttributes artifactAttributes = (DockerArtifactAttributes) that;
      return ComparatorUtils.compareDescending(this.getTag(), artifactAttributes.getTag());
    } else {
      throw new IllegalArgumentException("ArtifactAttributes list doesn't have all DockerArtifactAttributes elements.");
    }
  }
}