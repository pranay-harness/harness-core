package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.delegate.task.artifacts.ArtifactSourceConstants;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(ArtifactSourceConstants.ECR_NAME)
public class EcrArtifactSummary implements ArtifactSummary {
  String imagePath;
  String tag;

  @Override
  public String getType() {
    return ArtifactSourceConstants.ECR_NAME;
  }
}
