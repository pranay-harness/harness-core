package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingMap;
import io.harness.pms.ambiance.Ambiance;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NodeExecutionAncestorFunctor extends LateBindingMap {
  transient NodeExecutionsCache nodeExecutionsCache;
  transient OutcomeService outcomeService;
  transient ExecutionSweepingOutputService executionSweepingOutputService;
  transient Ambiance ambiance;
  transient Set<NodeExecutionEntityType> entityTypes;
  transient Map<String, String> groupAliases;

  @Override
  public synchronized Object get(Object key) {
    if (!(key instanceof String)) {
      return null;
    }

    NodeExecution startNodeExecution = findStartNodeExecution((String) key);
    return startNodeExecution == null ? null
                                      : NodeExecutionValue.builder()
                                            .nodeExecutionsCache(nodeExecutionsCache)
                                            .outcomeService(outcomeService)
                                            .executionSweepingOutputService(executionSweepingOutputService)
                                            .ambiance(ambiance)
                                            .startNodeExecution(startNodeExecution)
                                            .entityTypes(entityTypes)
                                            .build()
                                            .bind();
  }

  private NodeExecution findStartNodeExecution(String key) {
    if (groupAliases != null && groupAliases.containsKey(key)) {
      return findStartNodeExecutionByGroup(groupAliases.get(key));
    }

    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (nodeExecutionId == null) {
      return null;
    }

    for (NodeExecution currNodeExecution = nodeExecutionsCache.fetch(nodeExecutionId); currNodeExecution != null;
         currNodeExecution = nodeExecutionsCache.fetch(currNodeExecution.getParentId())) {
      if (!currNodeExecution.getNode().isSkipExpressionChain()
          && key.equals(currNodeExecution.getNode().getIdentifier())) {
        return currNodeExecution;
      }
    }
    return null;
  }

  private NodeExecution findStartNodeExecutionByGroup(String groupName) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (nodeExecutionId == null) {
      return null;
    }

    for (NodeExecution currNodeExecution = nodeExecutionsCache.fetch(nodeExecutionId); currNodeExecution != null;
         currNodeExecution = nodeExecutionsCache.fetch(currNodeExecution.getParentId())) {
      if (groupName.equals(currNodeExecution.getNode().getGroup())) {
        return currNodeExecution;
      }
    }
    return null;
  }
}
