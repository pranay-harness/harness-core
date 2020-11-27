package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.plan.PlanNode.PlanNodeBuilder;

public interface StepDependencyInstructor {
  /**
   * The instructor tells how to access the outcome required by the caller.
   * @param spec
   * @param planNodeBuilder
   * @param context
   */
  void attachDependency(StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, ExecutionPlanCreationContext context);

  /** Support function to identify which spec is applicable for which provider. */
  boolean supports(StepDependencySpec spec, ExecutionPlanCreationContext context);
}
