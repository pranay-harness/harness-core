package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.RunTestStepNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class RunTestStepPlanCreator extends CIPMSStepPlanCreatorV2<RunTestStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.RUN_TESTS.getDisplayName());
  }

  @Override
  public Class<RunTestStepNode> getFieldClass() {
    return RunTestStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, RunTestStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
