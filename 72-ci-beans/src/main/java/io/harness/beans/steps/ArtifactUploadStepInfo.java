package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ArtifactUploadInfo;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("UPLOAD_ARTIFACT")
@Data
@Value
@Builder
public class ArtifactUploadStepInfo implements StepInfo {
  @NotNull private CIStepType type = CIStepType.UPLOAD_ARTIFACT;
  @NotNull private StepType stateType = StepType.builder().type(CIStepType.UPLOAD_ARTIFACT.name()).build();
  private ArtifactUploadInfo artifactUploadInfo;

  @NotNull private String identifier;

  @Override
  public CIStepType getType() {
    return type;
  }

  @Override
  public String getStepIdentifier() {
    return identifier;
  }
}
