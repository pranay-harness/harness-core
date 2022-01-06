package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.provision.terraform.TerraformPlanStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class TerraformPlanStepPlanCreator extends CDPMSStepPlanCreatorV2<TerraformPlanStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.TERRAFORM_PLAN);
  }

  @Override
  public Class<TerraformPlanStepNode> getFieldClass() {
    return TerraformPlanStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, TerraformPlanStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
