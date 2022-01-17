package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.PluginStepNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class PluginStepPlanCreator extends CIPMSStepPlanCreatorV2<PluginStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.PLUGIN.getDisplayName());
  }

  @Override
  public Class<PluginStepNode> getFieldClass() {
    return PluginStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, PluginStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
