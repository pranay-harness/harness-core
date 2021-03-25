package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasSome;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingValue;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * NodeExecutionValue implements a LateBindingValue which matches expressions starting from startNodeExecution. If we
 * want to resolve fully qualified expressions, startNodeExecution should be null. OOtherwise, it should be the node
 * execution from where we want to start expression evaluation. It supports step parameters and outcomes in expressions.
 */
@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionValue implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  PmsOutcomeService pmsOutcomeService;
  PmsSweepingOutputService pmsSweepingOutputService;
  Ambiance ambiance;
  NodeExecution startNodeExecution;
  Set<NodeExecutionEntityType> entityTypes;

  @Override
  public Object bind() {
    Map<String, Object> map = new HashMap<>();
    addChildren(map, startNodeExecution == null ? null : startNodeExecution.getUuid());
    return NodeExecutionMap.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .pmsOutcomeService(pmsOutcomeService)
        .pmsSweepingOutputService(pmsSweepingOutputService)
        .ambiance(ambiance)
        .nodeExecution(startNodeExecution)
        .entityTypes(entityTypes)
        .children(map)
        .build();
  }

  private void addChildren(Map<String, Object> map, String nodeExecutionId) {
    List<NodeExecution> children = nodeExecutionsCache.fetchChildren(nodeExecutionId);
    for (NodeExecution child : children) {
      if (canAdd(child)) {
        addToMap(map, child);
        continue;
      }

      addChildren(map, child.getUuid());
    }
  }

  private boolean canAdd(NodeExecution nodeExecution) {
    return !nodeExecution.getNode().getSkipExpressionChain() && hasSome(nodeExecution.getNode().getIdentifier());
  }

  private void addToMap(Map<String, Object> map, NodeExecution nodeExecution) {
    String key = nodeExecution.getNode().getIdentifier();
    NodeExecutionValue childValue = NodeExecutionValue.builder()
                                        .nodeExecutionsCache(nodeExecutionsCache)
                                        .pmsOutcomeService(pmsOutcomeService)
                                        .pmsSweepingOutputService(pmsSweepingOutputService)
                                        .ambiance(ambiance)
                                        .startNodeExecution(nodeExecution)
                                        .entityTypes(entityTypes)
                                        .build();
    map.compute(key, (k, v) -> {
      if (v == null) {
        return childValue;
      }

      Object boundChild = childValue.bind();
      if (v instanceof List) {
        ((List<Object>) v).add(boundChild);
        return v;
      }

      Object boundV = (v instanceof NodeExecutionValue) ? ((NodeExecutionValue) v).bind() : v;
      return asList(boundV, boundChild);
    });
  }
}
