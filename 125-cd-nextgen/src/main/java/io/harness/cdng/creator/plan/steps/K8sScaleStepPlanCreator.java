package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.cdng.k8s.K8sScaleStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sScaleStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sScaleStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_SCALE);
  }

  @Override
  public Class<K8sScaleStepNode> getFieldClass() {
    return K8sScaleStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sScaleStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
