package io.harness.beans.steps.stepinfo.publish.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.WithImageConnector;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonDeserialize
public class DockerImageArtifact implements Artifact, WithImageConnector {
  @NotNull private String dockerImage;
  @NotNull private String tag;
  @NotNull private ArtifactConnector connector;
  @Override
  public Artifact.Type getType() {
    return Artifact.Type.DOCKER_IMAGE;
  }

  @Override
  public void setConnector(ArtifactConnector connector) {
    this.connector = connector;
  }
}
