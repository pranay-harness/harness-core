package io.harness.pipeline.plan.scratch.lib.creator;

import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.pipeline.plan.scratch.common.creator.PartialPlanCreator;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;

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

  public abstract Map<String, PlanCreationResponse> createPlanForChildrenNodes(YamlField field);

  @Override
  public PlanCreationResponse createPlanForField(YamlField field) {
    YamlNode yamlNode = field.getNode();
    String uuid = yamlNode.getUuid();
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    if (isStartingNode()) {
      finalResponse.setStartingNodeId(uuid);
    }

    Map<String, PlanCreationResponse> childrenResponses = createPlanForChildrenNodes(field);
    Set<String> childrenNodeIds = new HashSet<>();
    for (Map.Entry<String, PlanCreationResponse> entry : childrenResponses.entrySet()) {
      finalResponse.merge(entry.getValue());
      childrenNodeIds.add(entry.getKey());
    }

    PlanNode stagePlanNode =
        PlanNode.builder()
            .uuid(yamlNode.getUuid())
            .identifier(yamlNode.getIdentifier())
            .stepType(getStepType())
            .name(yamlNode.getNameOrIdentifier())
            .group(getGroup())
            .stepParameters(new MapStepParameters("childrenNodeIds", childrenNodeIds))
            .facilitatorObtainment(
                FacilitatorObtainment.builder()
                    .type(FacilitatorType.builder().type(isParallel() ? "CHILDREN" : "CHILD_CHAIN").build())
                    .build())
            .skipExpressionChain(false)
            .build();
    finalResponse.addNode(stagePlanNode);
    return finalResponse;
  }
}
