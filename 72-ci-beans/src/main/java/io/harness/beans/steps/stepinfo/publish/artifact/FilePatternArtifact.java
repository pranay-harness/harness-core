package io.harness.beans.steps.stepinfo.publish.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonDeserialize
public class FilePatternArtifact implements Artifact {
  @NotNull private String filePattern;
  @NotNull private Destination destination;

  @Override
  public ArtifactType getType() {
    return ArtifactType.FILE_PATTERN;
  }
}
