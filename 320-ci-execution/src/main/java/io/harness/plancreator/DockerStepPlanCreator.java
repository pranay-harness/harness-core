package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.BuildAndPushDockerNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class DockerStepPlanCreator extends CIPMSStepPlanCreatorV2<BuildAndPushDockerNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.DOCKER.getDisplayName());
  }

  @Override
  public Class<BuildAndPushDockerNode> getFieldClass() {
    return BuildAndPushDockerNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, BuildAndPushDockerNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
