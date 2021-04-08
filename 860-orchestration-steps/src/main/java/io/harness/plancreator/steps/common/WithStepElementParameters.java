package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.fasterxml.jackson.annotation.JsonIgnore;

@OwnedBy(PIPELINE)
public interface WithStepElementParameters {
  default StepParameters getStepParametersInfo(StepElementConfig stepElementConfig) {
    StepElementParametersBuilder stepParametersBuilder = StepParametersUtils.getStepParameters(stepElementConfig);
    stepParametersBuilder.spec(getStepSpecParameters());
    return stepParametersBuilder.build();
  }

  @JsonIgnore
  default StepSpecParameters getStepSpecParameters() {
    return null;
  }
}
