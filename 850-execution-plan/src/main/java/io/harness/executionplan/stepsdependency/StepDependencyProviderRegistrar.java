package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.core.ExecutionPlanCreationContext;

public interface StepDependencyProviderRegistrar {
  void registerStepDependencyInstructor(StepDependencyInstructor instructor, ExecutionPlanCreationContext context);
}
