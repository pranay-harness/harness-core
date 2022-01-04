package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.k8s.K8sRollingRollbackStepNode;
import io.harness.cdng.k8s.K8sRollingStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class K8sRollingRollbackStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sRollingRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_ROLLING_ROLLBACK);
  }

  @Override
  public Class<K8sRollingRollbackStepNode> getFieldClass() {
    return K8sRollingRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sRollingRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
