package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Redesign
@Value
public class NodeExecutionsCache {
  private static final String NULL_PARENT_ID = "__NULL_PARENT_ID__";

  NodeExecutionService nodeExecutionService;
  Ambiance ambiance;
  Map<String, NodeExecution> map;
  Map<String, List<String>> childrenMap;

  @Builder
  public NodeExecutionsCache(NodeExecutionService nodeExecutionService, Ambiance ambiance) {
    this.nodeExecutionService = nodeExecutionService;
    this.ambiance = ambiance;
    this.map = new HashMap<>();
    this.childrenMap = new HashMap<>();
  }

  public synchronized NodeExecution fetch(String nodeExecutionId) {
    if (nodeExecutionId == null) {
      return null;
    }
    if (map.containsKey(nodeExecutionId)) {
      return map.get(nodeExecutionId);
    }

    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    map.put(nodeExecutionId, nodeExecution);
    return nodeExecution;
  }

  public synchronized List<NodeExecution> fetchChildren(String parentId) {
    String childrenMapKey = parentId == null ? NULL_PARENT_ID : parentId;
    if (childrenMap.containsKey(childrenMapKey)) {
      List<String> ids = childrenMap.get(childrenMapKey);
      if (EmptyPredicate.isEmpty(ids)) {
        return Collections.emptyList();
      }

      return ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    List<NodeExecution> nodeExecutions =
        nodeExecutionService.fetchChildrenNodeExecutions(ambiance.getPlanExecutionId(), parentId);
    if (EmptyPredicate.isEmpty(nodeExecutions)) {
      childrenMap.put(parentId, Collections.emptyList());
      return Collections.emptyList();
    }

    nodeExecutions.forEach(nodeExecution -> map.put(nodeExecution.getUuid(), nodeExecution));
    childrenMap.put(parentId, nodeExecutions.stream().map(NodeExecution::getUuid).collect(Collectors.toList()));
    return nodeExecutions;
  }
}
