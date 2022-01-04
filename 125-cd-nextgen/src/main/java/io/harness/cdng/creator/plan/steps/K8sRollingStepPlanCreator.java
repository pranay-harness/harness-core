package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sRollingStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sRollingStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sRollingStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_ROLLING_DEPLOY);
  }

  @Override
  public Class<K8sRollingStepNode> getFieldClass() {
    return K8sRollingStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sRollingStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
