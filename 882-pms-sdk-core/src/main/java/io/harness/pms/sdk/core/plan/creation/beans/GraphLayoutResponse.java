package io.harness.pms.sdk.core.plan.creation.beans;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.*;
import io.harness.pms.sdk.core.plan.creation.mappers.PlanNodeProtoMapper;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphLayoutResponse {
  @Builder.Default Map<String, GraphLayoutNode> layoutNodes = new HashMap<>();
  String startingNodeId;

  public void addLayoutNodes(Map<String, GraphLayoutNode> layoutNodes) {
    if (EmptyPredicate.isEmpty(layoutNodes)) {
      return;
    }
    layoutNodes.values().forEach(this::addLayoutNode);
  }

  public void addLayoutNode(GraphLayoutNode layoutNode) {
    if (layoutNode == null) {
      return;
    }
    if (layoutNodes == null) {
      layoutNodes = new HashMap<>();
    } else if (!(layoutNodes instanceof HashMap)) {
      layoutNodes = new HashMap<>(layoutNodes);
    }
    layoutNodes.put(layoutNode.getNodeUUID(), layoutNode);
  }

  public void mergeStartingNodeId(String otherStartingNodeId) {
    if (EmptyPredicate.isEmpty(otherStartingNodeId)) {
      return;
    }
    if (EmptyPredicate.isEmpty(startingNodeId)) {
      startingNodeId = otherStartingNodeId;
      return;
    }
    if (!startingNodeId.equals(otherStartingNodeId)) {
      throw new InvalidRequestException(
          String.format("Received different set of starting nodes: %s and %s", startingNodeId, otherStartingNodeId));
    }
  }

  public GraphLayoutInfo getLayoutNodeInfo() {
    GraphLayoutInfo.Builder builder = GraphLayoutInfo.newBuilder();
    if (startingNodeId != null) {
      builder.setStartingNodeId(startingNodeId);
    }
    if (layoutNodes != null) {
      builder.putAllLayoutNodes(layoutNodes);
    }
    return builder.build();
  }
}
