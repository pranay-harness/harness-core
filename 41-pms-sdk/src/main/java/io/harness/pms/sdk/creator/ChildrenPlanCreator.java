package io.harness.pms.sdk.creator;

import io.harness.pms.plan.common.creator.PlanCreationContext;
import io.harness.pms.plan.common.creator.PlanCreationResponse;
import io.harness.pms.plan.common.yaml.YamlField;
import io.harness.pms.plan.common.yaml.YamlNode;
import io.harness.pms.sdk.io.MapStepParameters;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.steps.StepType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ChildrenPlanCreator implements PartialPlanCreator {
  public abstract boolean isParallel();

  public abstract StepType getStepType();

  public String getGroup() {
    return null;
  }

  public boolean isStartingNode() {
    return false;
  }

  public abstract Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, YamlField field);

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field) {
    YamlNode yamlNode = field.getNode();
    String uuid = yamlNode.getUuid();
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (isStartingNode()) {
      finalResponse.setStartingNodeId(uuid);
    }

    Map<String, PlanCreationResponse> childrenResponses = createPlanForChildrenNodes(ctx, field);
    Set<String> childrenNodeIds = new HashSet<>();
    for (Map.Entry<String, PlanCreationResponse> entry : childrenResponses.entrySet()) {
      finalResponse.merge(entry.getValue());
      childrenNodeIds.add(entry.getKey());
    }

    PlanNode.Builder stagePlanNodeBuilder =
        PlanNode.newBuilder()
            .setUuid(yamlNode.getUuid())
            .setIdentifier(yamlNode.getIdentifier())
            .setStepType(getStepType())
            .setName(yamlNode.getNameOrIdentifier())
            .setStepParameters(ctx.toByteString(new MapStepParameters("childrenNodeIds", childrenNodeIds)))
            .addFacilitatorObtainments(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(isParallel() ? "CHILDREN" : "CHILD_CHAIN").build())
                    .build())
            .setSkipExpressionChain(false);

    String group = getGroup();
    if (group != null) {
      stagePlanNodeBuilder.setGroup(group);
    }
    finalResponse.addNode(stagePlanNodeBuilder.build());
    return finalResponse;
  }
}
