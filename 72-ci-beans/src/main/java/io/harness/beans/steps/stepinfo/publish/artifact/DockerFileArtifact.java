package io.harness.beans.steps.stepinfo.publish.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonDeserialize
public class DockerFileArtifact implements Artifact {
  @NotNull private String dockerFile;
  @NotNull private String image;
  @NotNull private String tag;
  private List<Map<String, String>> buildArguments;
  @NotNull private Destination destination;

  @Override
  public ArtifactType getType() {
    return ArtifactType.DOCKER_FILE;
  }
}
