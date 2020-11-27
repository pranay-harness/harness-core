package io.harness.executionplan.plancreator.beans;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepParameters;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

public interface GenericStepInfo extends StepParameters, WithIdentifier {
  @JsonIgnore String getDisplayName();
  @JsonIgnore StepType getStepType();
  @JsonIgnore String getFacilitatorType();
  @JsonIgnore
  default StepParameters getStepParameters() {
    return this;
  }

  @JsonIgnore
  /** Get the input step dependencies for the current step. */
  default Map<String, StepDependencySpec> getInputStepDependencyList(ExecutionPlanCreationContext context) {
    return new HashMap<>();
  }

  @JsonIgnore
  /** Register instructors from this step, to be available for other steps. */
  default void registerStepDependencyInstructors(
      StepDependencyService stepDependencyService, ExecutionPlanCreationContext context, String nodeId) {
    // Do nothing.
  }
}
