package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.provision.terraform.TerraformDestroyStepNode;
import io.harness.cdng.provision.terraform.TerraformPlanStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class TerraformDestroyStepPlanCreator extends CDPMSStepPlanCreatorV2<TerraformDestroyStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.TERRAFORM_DESTROY);
  }

  @Override
  public Class<TerraformDestroyStepNode> getFieldClass() {
    return TerraformDestroyStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, TerraformDestroyStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
