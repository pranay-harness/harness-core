package io.harness.pms.sdk.core.plan.creation.beans;

import io.harness.async.AsyncCreatorResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class PlanCreationResponse implements AsyncCreatorResponse {
  @Singular Map<String, PlanNode> nodes;
  @Singular Map<String, YamlField> dependencies;
  @Singular("contextMap") Map<String, PlanCreationContextValue> contextMap;
  GraphLayoutResponse graphLayoutResponse;

  String startingNodeId;
  @Singular List<String> errorMessages;

  public void merge(PlanCreationResponse other) {
    addNodes(other.getNodes());
    addDependencies(other.getDependencies());
    mergeStartingNodeId(other.getStartingNodeId());
    mergeContext(other.getContextMap());
    mergeLayoutNodeInfo(other.getGraphLayoutResponse());
  }

  public void mergeContext(Map<String, PlanCreationContextValue> contextMap) {
    if (EmptyPredicate.isEmpty(contextMap)) {
      return;
    }
    for (Map.Entry<String, PlanCreationContextValue> entry : contextMap.entrySet()) {
      putContextValue(entry.getKey(), entry.getValue());
    }
  }

  public void mergeLayoutNodeInfo(GraphLayoutResponse layoutNodeInfo) {
    if (layoutNodeInfo == null) {
      return;
    }
    if (graphLayoutResponse == null) {
      graphLayoutResponse = layoutNodeInfo;
      return;
    }
    graphLayoutResponse.mergeStartingNodeId(layoutNodeInfo.getStartingNodeId());
    graphLayoutResponse.addLayoutNodes(layoutNodeInfo.getLayoutNodes());
  }

  public void addNodes(Map<String, PlanNode> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return;
    }
    newNodes.values().forEach(this::addNode);
  }

  public void addNode(PlanNode newNode) {
    if (nodes == null) {
      nodes = new HashMap<>();
    } else if (!(nodes instanceof HashMap)) {
      nodes = new HashMap<>(nodes);
    }

    // TODO: Add logic to update only if newNode has a more recent version.
    nodes.put(newNode.getUuid(), newNode);
    if (dependencies != null) {
      dependencies.remove(newNode.getUuid());
    }
  }

  public void addDependencies(Map<String, YamlField> fields) {
    if (EmptyPredicate.isEmpty(fields)) {
      return;
    }
    fields.values().forEach(this::addDependency);
  }

  public void putContextValue(String key, PlanCreationContextValue value) {
    if (contextMap != null && contextMap.containsKey(key)) {
      return;
    }

    if (contextMap == null) {
      contextMap = new HashMap<>();
    } else if (!(contextMap instanceof HashMap)) {
      contextMap = new HashMap<>(contextMap);
    }
    contextMap.put(key, value);
  }

  public void addDependency(YamlField field) {
    String nodeId = field.getNode().getUuid();
    if ((dependencies != null && dependencies.containsKey(nodeId)) || (nodes != null && nodes.containsKey(nodeId))) {
      return;
    }

    if (dependencies == null) {
      dependencies = new HashMap<>();
    } else if (!(dependencies instanceof HashMap)) {
      dependencies = new HashMap<>(dependencies);
    }
    dependencies.put(nodeId, field);
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
}
