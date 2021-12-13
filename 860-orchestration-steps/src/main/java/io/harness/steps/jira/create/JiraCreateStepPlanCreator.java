package io.harness.steps.jira.create;

import io.harness.plancreator.steps.internal.PMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;

public class JiraCreateStepPlanCreator extends PMSStepPlanCreatorV2<JiraCreateStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.JIRA_CREATE);
  }

  @Override
  public Class<JiraCreateStepNode> getFieldClass() {
    return JiraCreateStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, JiraCreateStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
