package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sApplyStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sApplyStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_APPLY);
  }

  @Override
  public Class<K8sApplyStepNode> getFieldClass() {
    return K8sApplyStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sApplyStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
