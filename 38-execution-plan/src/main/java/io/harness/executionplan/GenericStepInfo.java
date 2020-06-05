package io.harness.executionplan;

import io.harness.state.StepType;
import io.harness.state.io.StepParameters;

public interface GenericStepInfo extends StepParameters {
  String getName();
  String getIdentifier();
  StepType getStepType();
  String getFacilitatorType();
}
