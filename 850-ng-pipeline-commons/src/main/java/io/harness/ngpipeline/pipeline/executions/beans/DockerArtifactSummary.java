package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.ngpipeline.artifact.bean.ArtifactTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(ArtifactTypes.DOCKER_ARTIFACT)
public class DockerArtifactSummary implements ArtifactSummary {
  String imagePath;
  String tag;

  @Override
  public String getType() {
    return ArtifactTypes.DOCKER_ARTIFACT;
  }
}
