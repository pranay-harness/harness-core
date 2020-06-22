package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.k8s.K8sRollingStep;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import io.harness.state.io.K8sRollingStepParameters;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("k8sRolling")
public class K8sRollingStepInfo implements CDStepInfo {
  private String displayName;
  private String identifier;
  private K8sRollingStepParameters k8sRolling;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.TASK_CHAIN;
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }
}
