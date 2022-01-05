package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.cdng.k8s.K8sCanaryDeleteStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sCanaryDeleteStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sCanaryDeleteStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_CANARY_DELETE);
  }

  @Override
  public Class<K8sCanaryDeleteStepNode> getFieldClass() {
    return K8sCanaryDeleteStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sCanaryDeleteStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
