package io.harness.pms.sdk.core.plan.creation.creators;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.GraphLayoutResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ChildrenPlanCreator<T> implements PartialPlanCreator<T> {
  public String getStartingNodeId(T field) {
    return null;
  }

  public abstract LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, T config);

  public abstract PlanNode createPlanForParentNode(PlanCreationContext ctx, T config, List<String> childrenNodeIds);

  public GraphLayoutResponse getLayoutNodeInfo(PlanCreationContext ctx, T config) {
    return GraphLayoutResponse.builder().build();
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, T config) {
    PlanCreationResponse finalResponse = PlanCreationResponse.builder().build();
    String startingNodeId = getStartingNodeId(config);
    if (hasSome(startingNodeId)) {
      finalResponse.setStartingNodeId(startingNodeId);
    }

    LinkedHashMap<String, PlanCreationResponse> childrenResponses = createPlanForChildrenNodes(ctx, config);
    List<String> childrenNodeIds = new LinkedList<>();
    for (Map.Entry<String, PlanCreationResponse> entry : childrenResponses.entrySet()) {
      finalResponse.merge(entry.getValue());
      childrenNodeIds.add(entry.getKey());
    }

    finalResponse.addNode(createPlanForParentNode(ctx, config, childrenNodeIds));
    finalResponse.setGraphLayoutResponse(getLayoutNodeInfo(ctx, config));
    return finalResponse;
  }
}
