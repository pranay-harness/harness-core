package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;

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
    addDependencies(builder, other.getDeps());
    mergeStartingNodeId(builder, other.getStartingNodeId());
    mergeContext(builder, other.getContextMap());
    mergeLayoutNodeInfo(builder, other);
  }

  public void mergeContext(
      PlanCreationBlobResponse.Builder builder, Map<String, PlanCreationContextValue> contextValueMap) {
    if (EmptyPredicate.isEmpty(contextValueMap)) {
      return;
    }
    builder.putAllContext(contextValueMap);
  }

  public void addNodes(PlanCreationBlobResponse.Builder builder, Map<String, PlanNodeProto> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return;
    }
    newNodes.values().forEach(newNode -> addNode(builder, newNode));
  }

  public void addNode(PlanCreationBlobResponse.Builder builder, PlanNodeProto newNode) {
    builder.putNodes(newNode.getUuid(), newNode);
    removeDependency(builder, newNode.getUuid());
  }

  public void addDependencies(PlanCreationBlobResponse.Builder builder, Dependencies deps) {
    if (deps == null || EmptyPredicate.isEmpty(deps.getDependenciesMap())) {
      return;
    }
    deps.getDependenciesMap().forEach((key, value) -> addDependency(builder, key, value));
  }

  public void addDependency(PlanCreationBlobResponse.Builder builder, String nodeId, String path) {
    if (builder.containsNodes(nodeId)) {
      return;
    }
    builder.setDeps(builder.getDeps().toBuilder().putDependencies(nodeId, path).build());
  }

  public void removeDependency(PlanCreationBlobResponse.Builder builder, String nodeId) {
    builder.setDeps(builder.getDeps().toBuilder().removeDependencies(nodeId).build());
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
