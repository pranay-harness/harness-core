package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.WithFileConnector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonDeserialize
@TypeAlias("filePatternArtifact")
public class FilePatternArtifact implements Artifact, WithFileConnector {
  @NotNull private String filePattern;
  @NotNull private ArtifactConnector connector;
  @Override
  public Artifact.Type getType() {
    return Artifact.Type.FILE_PATTERN;
  }

  @Override
  public void setConnector(ArtifactConnector connector) {
    this.connector = connector;
  }
}
