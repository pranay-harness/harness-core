package io.harness.pms.sdk.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.plan.PlanNode;
import io.harness.pms.plan.creation.PlanCreationContext;
import io.harness.pms.plan.creation.PlanCreationResponse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ChildrenPlanCreator<T> implements PartialPlanCreator<T> {
  public String getStartingNodeId(T field) {
    return null;
  }

  public abstract Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, T field);

  public abstract PlanNode createPlanForParentNode(PlanCreationContext ctx, T field, Set<String> childrenNodeIds);

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, T field) {
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    String startingNodeId = getStartingNodeId(field);
    if (EmptyPredicate.isNotEmpty(startingNodeId)) {
      finalResponse.setStartingNodeId(startingNodeId);
    }

    Map<String, PlanCreationResponse> childrenResponses = createPlanForChildrenNodes(ctx, field);
    Set<String> childrenNodeIds = new HashSet<>();
    for (Map.Entry<String, PlanCreationResponse> entry : childrenResponses.entrySet()) {
      finalResponse.merge(entry.getValue());
      childrenNodeIds.add(entry.getKey());
    }

    finalResponse.addNode(createPlanForParentNode(ctx, field, childrenNodeIds));
    return finalResponse;
  }
}
