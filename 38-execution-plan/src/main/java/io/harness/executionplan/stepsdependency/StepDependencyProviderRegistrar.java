package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.core.CreateExecutionPlanContext;

public interface StepDependencyProviderRegistrar {
  void registerStepDependencyInstructor(StepDependencyInstructor instructor, CreateExecutionPlanContext context);
}
