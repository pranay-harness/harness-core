package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.YamlFieldBlob;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PlanCreationBlobResponseUtils {
  public void merge(PlanCreationBlobResponse.Builder builder, PlanCreationBlobResponse other) {
    if (other == null) {
      return;
    }
    addNodes(builder, other.getNodesMap());
    addDependencies(builder, other.getDependenciesMap());
    mergeStartingNodeId(builder, other.getStartingNodeId());
    mergeContext(builder, other.getContextMap());
    mergeLayoutNodeInfo(builder, other);
  }

  public PlanCreationBlobResponse mergeContext(
      PlanCreationBlobResponse.Builder builder, Map<String, PlanCreationContextValue> contextValueMap) {
    if (EmptyPredicate.isEmpty(contextValueMap)) {
      return builder.build();
    }
    builder.putAllContext(contextValueMap);
    return builder.build();
  }

  public PlanCreationBlobResponse addNodes(
      PlanCreationBlobResponse.Builder builder, Map<String, PlanNodeProto> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return builder.build();
    }
    newNodes.values().forEach(newNode -> addNode(builder, newNode));
    return builder.build();
  }

  public PlanCreationBlobResponse addNode(PlanCreationBlobResponse.Builder builder, PlanNodeProto newNode) {
    // TODO: Add logic to update only if newNode has a more recent version.
    builder.putNodes(newNode.getUuid(), newNode);
    builder.removeDependencies(newNode.getUuid());
    return builder.build();
  }

  public PlanCreationBlobResponse addDependencies(
      PlanCreationBlobResponse.Builder builder, Map<String, YamlFieldBlob> fieldBlobs) {
    if (EmptyPredicate.isEmpty(fieldBlobs)) {
      return builder.build();
    }
    fieldBlobs.forEach((key, value) -> addDependency(builder, key, value));
    return builder.build();
  }

  public PlanCreationBlobResponse addDependency(
      PlanCreationBlobResponse.Builder builder, String nodeId, YamlFieldBlob fieldBlob) {
    if (builder.containsNodes(nodeId)) {
      return builder.build();
    }

    builder.putDependencies(nodeId, fieldBlob);
    return builder.build();
  }

  public void mergeStartingNodeId(PlanCreationBlobResponse.Builder builder, String otherStartingNodeId) {
    if (EmptyPredicate.isEmpty(otherStartingNodeId)) {
      return;
    }
    if (EmptyPredicate.isEmpty(builder.getStartingNodeId())) {
      builder.setStartingNodeId(otherStartingNodeId);
      return;
    }
    if (!builder.getStartingNodeId().equals(otherStartingNodeId)) {
      throw new InvalidRequestException(String.format(
          "Received different set of starting nodes: %s and %s", builder.getStartingNodeId(), otherStartingNodeId));
    }
  }

  public void mergeLayoutNodeInfo(PlanCreationBlobResponse.Builder builder, PlanCreationBlobResponse response) {
    if (response.getGraphLayoutInfo() != null) {
      String otherStartingNodeId = response.getGraphLayoutInfo().getStartingNodeId();
      GraphLayoutInfo.Builder layoutNodeInfo = GraphLayoutInfo.newBuilder();
      if (EmptyPredicate.isEmpty(builder.getGraphLayoutInfo().getStartingNodeId())) {
        layoutNodeInfo.setStartingNodeId(otherStartingNodeId);
      } else {
        layoutNodeInfo.setStartingNodeId(builder.getGraphLayoutInfo().getStartingNodeId());
      }
      Map<String, GraphLayoutNode> layoutMap = new HashMap<>();
      if (builder.getGraphLayoutInfo() != null) {
        layoutMap = builder.getGraphLayoutInfo().getLayoutNodesMap();
      }
      if (!(layoutMap instanceof HashMap)) {
        layoutMap = new HashMap<>(layoutMap);
      }
      if (response.getGraphLayoutInfo().getLayoutNodesMap() != null) {
        layoutMap.putAll(response.getGraphLayoutInfo().getLayoutNodesMap());
      }

      layoutNodeInfo.putAllLayoutNodes(layoutMap);
      builder.setGraphLayoutInfo(layoutNodeInfo);
    }
  }
}
