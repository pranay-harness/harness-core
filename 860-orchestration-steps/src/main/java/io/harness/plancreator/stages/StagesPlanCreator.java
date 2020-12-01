package io.harness.plancreator.stages;

import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;

import java.util.*;

public class StagesPlanCreator extends ChildrenPlanCreator<StagesConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, StagesConfig field) {
    return null;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, StagesConfig field, Set<String> childrenNodeIds) {
    return null;
  }

  @Override
  public Class<StagesConfig> getFieldClass() {
    return StagesConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stages", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
