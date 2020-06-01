package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.services.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingMap;
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
  transient Ambiance ambiance;

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
                                            .ambiance(ambiance)
                                            .startNodeExecution(startNodeExecution)
                                            .build()
                                            .bind();
  }

  private NodeExecution findStartNodeExecution(String key) {
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();
    if (nodeExecutionId == null) {
      return null;
    }

    for (NodeExecution currNodeExecution = nodeExecutionsCache.fetch(nodeExecutionId); currNodeExecution != null;
         currNodeExecution = nodeExecutionsCache.fetch(currNodeExecution.getParentId())) {
      if (key.equals(currNodeExecution.getNode().getIdentifier())) {
        return currNodeExecution;
      }
    }
    return null;
  }
}
