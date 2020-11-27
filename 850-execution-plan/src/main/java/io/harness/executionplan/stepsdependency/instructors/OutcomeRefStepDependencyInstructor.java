package io.harness.executionplan.stepsdependency.instructors;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.refObjects.RefObjectUtil;

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
      StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, ExecutionPlanCreationContext context) {
    planNodeBuilder.refObject(RefObjectUtil.getOutcomeRefObject(outcomeExpression, providerPlanNodeId, key));
  }

  @Override
  public boolean supports(StepDependencySpec spec, ExecutionPlanCreationContext context) {
    if (spec instanceof KeyAware) {
      KeyAware keyAware = (KeyAware) spec;
      return keyAware.getKey().equals(key);
    }
    return false;
  }
}
