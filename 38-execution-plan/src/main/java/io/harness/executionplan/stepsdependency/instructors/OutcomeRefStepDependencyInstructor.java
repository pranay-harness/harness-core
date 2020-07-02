package io.harness.executionplan.stepsdependency.instructors;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.references.OutcomeRefObject;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class OutcomeRefStepDependencyInstructor implements StepDependencyInstructor {
  @NonNull String key;
  @NonNull String providerPlanNodeId;
  @NonNull String outcomeExpression;

  @Override
  public void attachDependency(
      StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, CreateExecutionPlanContext context) {
    planNodeBuilder.refObject(
        OutcomeRefObject.builder().name(outcomeExpression).producerId(providerPlanNodeId).key(key).build());
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
