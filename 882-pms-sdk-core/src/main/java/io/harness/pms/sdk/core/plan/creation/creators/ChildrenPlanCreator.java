package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import java.util.*;

public abstract class ChildrenPlanCreator<T> implements PartialPlanCreator<T> {
  public String getStartingNodeId(T field) {
    return null;
  }

  public abstract Map<String, PlanCreationResponse> createPlanForChildrenNodes(PlanCreationContext ctx, T config);

  public abstract PlanNode createPlanForParentNode(PlanCreationContext ctx, T config, List<String> childrenNodeIds);

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, T config) {
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    String startingNodeId = getStartingNodeId(config);
    if (EmptyPredicate.isNotEmpty(startingNodeId)) {
      finalResponse.setStartingNodeId(startingNodeId);
    }

    Map<String, PlanCreationResponse> childrenResponses = createPlanForChildrenNodes(ctx, config);
    List<String> childrenNodeIds = new LinkedList<>();
    for (Map.Entry<String, PlanCreationResponse> entry : childrenResponses.entrySet()) {
      finalResponse.merge(entry.getValue());
      childrenNodeIds.add(entry.getKey());
    }

    finalResponse.addNode(createPlanForParentNode(ctx, config, childrenNodeIds));
    return finalResponse;
  }
}
