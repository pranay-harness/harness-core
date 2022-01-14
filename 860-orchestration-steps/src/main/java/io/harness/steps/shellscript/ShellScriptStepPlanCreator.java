package io.harness.steps.shellscript;

import io.harness.plancreator.steps.internal.PMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;

public class ShellScriptStepPlanCreator extends PMSStepPlanCreatorV2<ShellScriptStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.SHELL_SCRIPT);
  }

  @Override
  public Class<ShellScriptStepNode> getFieldClass() {
    return ShellScriptStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, ShellScriptStepNode field) {
    return super.createPlanForField(ctx, field);
  }
}
