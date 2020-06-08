package io.harness.executionplan.plancreator.beans;

import io.harness.state.StepType;
import io.harness.state.io.StepParameters;

public interface GenericStepInfo extends StepParameters {
  String getName();
  String getIdentifier();
  StepType getStepType();
  String getFacilitatorType();
  default StepParameters getStepParameters() {
    return this;
  }
}
