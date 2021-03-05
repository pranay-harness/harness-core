package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
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

  /**
   * Fetches a list of children for a particular parent Id.
   *
   * If parentId found in cache {@link NodeExecutionsCache#childrenMap} return list of nodes by
   * querying the {@link NodeExecutionsCache#map}
   *
   * Adds all the children to the {@link NodeExecutionsCache#map} and populates
   * {@link NodeExecutionsCache#childrenMap} with parentId => List#childIds
   *
   */
  public synchronized List<NodeExecution> fetchChildren(String parentId) {
    String childrenMapKey = parentId == null ? NULL_PARENT_ID : parentId;
    if (childrenMap.containsKey(childrenMapKey)) {
      List<String> ids = childrenMap.get(childrenMapKey);
      if (EmptyPredicate.isEmpty(ids)) {
        return Collections.emptyList();
      }

      return ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    List<NodeExecution> childExecutions =
        nodeExecutionService.fetchChildrenNodeExecutions(ambiance.getPlanExecutionId(), parentId);
    if (EmptyPredicate.isEmpty(childExecutions)) {
      childrenMap.put(parentId, Collections.emptyList());
      return Collections.emptyList();
    }

    childExecutions.forEach(childExecution -> map.put(childExecution.getUuid(), childExecution));
    childrenMap.put(parentId, childExecutions.stream().map(NodeExecution::getUuid).collect(Collectors.toList()));
    return childExecutions;
  }

  public Map<String, Object> extractFinalStepParameters(NodeExecution nodeExecution) {
    if (nodeExecution.getResolvedStepParameters() != null) {
      Map<String, Object> stepParameters =
          NodeExecutionUtils.extractAndProcessStepParameters(nodeExecution.getResolvedStepParameters().toJson());
      if (stepParameters != null) {
        return stepParameters;
      }
    }
    return NodeExecutionUtils.extractAndProcessStepParameters(nodeExecution.getNode().getStepParameters());
  }
}
