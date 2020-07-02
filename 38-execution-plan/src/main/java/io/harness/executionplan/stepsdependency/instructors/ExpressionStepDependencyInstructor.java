package io.harness.executionplan.stepsdependency.instructors;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExpressionStepDependencyInstructor implements StepDependencyInstructor, KeyAware {
  String key;

  @Override
  public void attachDependency(
      StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, CreateExecutionPlanContext context) {
    // Do nothing.
  }

  @Override
  public boolean supports(StepDependencySpec spec, CreateExecutionPlanContext context) {
    if (spec instanceof KeyAware) {
      KeyAware keyAware = (KeyAware) spec;
      return keyAware.getKey().equals(key);
    }
    return false;
  }
}
