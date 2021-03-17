package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.serializer.json.JsonOrchestrationIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithRollbackInfo {
  @JsonIgnore
  @JsonOrchestrationIgnore
  StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo);

  default boolean validateStageFailureStrategy() {
    return true;
  }
}
