package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.helm.HelmDeployStepNode;
import io.harness.cdng.helm.HelmRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class HelmRollbackStepPlanCreatorV2 extends CDPMSStepPlanCreatorV2<HelmRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.HELM_ROLLBACK);
  }

  @Override
  public Class<HelmRollbackStepNode> getFieldClass() {
    return HelmRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, HelmRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
