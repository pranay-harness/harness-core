package io.harness.pms.sdk.core.plan.creation.beans;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;

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
    if (hasNone(layoutNodes)) {
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
    if (hasNone(otherStartingNodeId)) {
      return;
    }
    if (hasNone(startingNodeId)) {
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
