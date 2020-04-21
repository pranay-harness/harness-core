package io.harness.beans.steps;

import io.harness.beans.ArtifactUploadInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Data
@Value
@Builder
public class CIArtifactUploadStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.UPLOAD_ARTIFACT;

  private ArtifactUploadInfo artifactUploadInfo;

  @NotNull private String name;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public String getStepName() {
    return name;
  }
}
