package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.WithImageConnector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@JsonDeserialize
public class DockerFileArtifact implements Artifact, WithImageConnector {
  @NotNull private String dockerFile;
  @NotNull private String context;
  @NotNull private String image;
  @NotNull private String tag;
  private List<BuildArgument> buildArguments;
  @NotNull private ArtifactConnector connector;

  @Override
  public Type getType() {
    return Type.DOCKER_FILE;
  }

  @Override
  public void setConnector(ArtifactConnector connector) {
    this.connector = connector;
  }

  @Value
  @Builder
  public static class BuildArgument {
    @NotNull private String key;
    @NotNull private String value;
  }
}
