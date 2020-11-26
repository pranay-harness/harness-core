package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.WithImageConnector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonDeserialize
@TypeAlias("DockerImageArtifact")
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
