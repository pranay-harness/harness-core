package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.cdng.provision.terraform.TerraformApplyStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class TerraformApplyStepPlanCreator extends CDPMSStepPlanCreatorV2<TerraformApplyStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.TERRAFORM_APPLY);
  }

  @Override
  public Class<TerraformApplyStepNode> getFieldClass() {
    return TerraformApplyStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, TerraformApplyStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
