package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.helm.HelmDeployStepNode;
import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class HelmDeployStepPlanCreatorV2 extends CDPMSStepPlanCreatorV2<HelmDeployStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.HELM_DEPLOY);
  }

  @Override
  public Class<HelmDeployStepNode> getFieldClass() {
    return HelmDeployStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, HelmDeployStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
