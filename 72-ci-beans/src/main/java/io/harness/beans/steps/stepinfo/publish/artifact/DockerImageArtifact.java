package io.harness.beans.steps.stepinfo.publish.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonDeserialize
public class DockerImageArtifact implements Artifact {
  @NotNull private String dockerImage;
  @NotNull private String tag;
  @NotNull private Destination destination;

  @Override
  public ArtifactType getType() {
    return ArtifactType.DOCKER_IMAGE;
  }
}
