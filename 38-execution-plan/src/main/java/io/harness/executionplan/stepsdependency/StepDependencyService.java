package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.plan.PlanNode.PlanNodeBuilder;

public interface StepDependencyService extends StepDependencyResolver, StepDependencyProviderRegistrar {
  /**
   * The instructor tells how to access the outcome required by the caller.
   * @param spec
   * @param planNodeBuilder
   * @param context
   */
  void attachDependency(StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, ExecutionPlanCreationContext context);
}
