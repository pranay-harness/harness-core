package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.fasterxml.jackson.annotation.JsonIgnore;

@OwnedBy(CDC)
public interface WithRollbackInfo {
  @JsonIgnore StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo);

  default boolean validateStageFailureStrategy() {
    return true;
  }
}
