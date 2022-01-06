package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.provision.terraform.TerraformRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class TerraformRollbackStepPlanCreator extends CDPMSStepPlanCreatorV2<TerraformRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.TERRAFORM_ROLLBACK);
  }

  @Override
  public Class<TerraformRollbackStepNode> getFieldClass() {
    return TerraformRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, TerraformRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
