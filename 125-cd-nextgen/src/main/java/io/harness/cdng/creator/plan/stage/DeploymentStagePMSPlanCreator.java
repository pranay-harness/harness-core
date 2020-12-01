package io.harness.cdng.creator.plan.stage;

import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStagePMSPlanCreator extends ChildrenPlanCreator<DeploymentStageConfig> {
  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DeploymentStageConfig field) {
    return null;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DeploymentStageConfig field, Set<String> childrenNodeIds) {
    return null;
  }

  @Override
  public Class<DeploymentStageConfig> getFieldClass() {
    return null;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("deployment"));
  }
}
