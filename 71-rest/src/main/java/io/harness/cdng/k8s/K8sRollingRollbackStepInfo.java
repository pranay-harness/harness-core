package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("k8sRollingRollback")
public class K8sRollingRollbackStepInfo implements CDStepInfo {
  private String displayName;
  private String identifier;
  private K8sRollingRollbackStepParameters k8sRollingRollback;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.TASK;
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }
}
