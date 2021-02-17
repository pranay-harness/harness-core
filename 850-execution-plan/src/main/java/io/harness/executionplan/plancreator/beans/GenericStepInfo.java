package io.harness.executionplan.plancreator.beans;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface GenericStepInfo extends StepParameters, WithIdentifier {
  @JsonIgnore String getDisplayName();
  @JsonIgnore StepType getStepType();
  @JsonIgnore String getFacilitatorType();
}
