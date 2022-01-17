package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.BuildAndPushGCRNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class BuildAndPushGCRStepPlanCreator extends CIPMSStepPlanCreatorV2<BuildAndPushGCRNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.GCR.getDisplayName());
  }

  @Override
  public Class<BuildAndPushGCRNode> getFieldClass() {
    return BuildAndPushGCRNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, BuildAndPushGCRNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
