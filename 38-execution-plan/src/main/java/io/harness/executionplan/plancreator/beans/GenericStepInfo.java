package io.harness.executionplan.plancreator.beans;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.yaml.core.intfc.WithIdentifier;

import java.util.HashMap;
import java.util.Map;

public interface GenericStepInfo extends StepParameters, WithIdentifier {
  String getDisplayName();
  StepType getStepType();
  String getFacilitatorType();
  default StepParameters getStepParameters() {
    return this;
  }

  /** Get the input step dependencies for the current step. */
  default Map<String, StepDependencySpec> getInputStepDependencyList(CreateExecutionPlanContext context) {
    return new HashMap<>();
  }

  /** Register instructors from this step, to be available for other steps. */
  default void registerStepDependencyInstructors(
      StepDependencyService stepDependencyService, CreateExecutionPlanContext context, String nodeId) {
    // Do nothing.
  }
}
