package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ArtifactUploadInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("UPLOAD_ARTIFACT")
@Data
@Value
@Builder
public class CIArtifactUploadStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.UPLOAD_ARTIFACT;
  @NotNull private StateType stateType = StateType.builder().type(StepType.UPLOAD_ARTIFACT.name()).build();
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
