package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sDeleteStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sDeleteStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sDeleteStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_DELETE);
  }

  @Override
  public Class<K8sDeleteStepNode> getFieldClass() {
    return K8sDeleteStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sDeleteStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
