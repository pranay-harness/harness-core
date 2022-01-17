package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.BuildAndPushECRNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class BuildAndPushECRStepPlanCreator extends CIPMSStepPlanCreatorV2<BuildAndPushECRNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.ECR.getDisplayName());
  }

  @Override
  public Class<BuildAndPushECRNode> getFieldClass() {
    return BuildAndPushECRNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, BuildAndPushECRNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
