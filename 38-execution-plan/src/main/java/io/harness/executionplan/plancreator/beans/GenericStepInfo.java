package io.harness.executionplan.plancreator.beans;

import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.yaml.core.intfc.WithIdentifier;

public interface GenericStepInfo extends StepParameters, WithIdentifier {
  String getDisplayName();
  StepType getStepType();
  String getFacilitatorType();
  default StepParameters getStepParameters() {
    return this;
  }
}
