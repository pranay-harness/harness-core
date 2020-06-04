package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.services.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingValue;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionChildFunctor implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  OutcomeService outcomeService;
  ExecutionSweepingOutputService executionSweepingOutputService;
  Ambiance ambiance;
  Set<NodeExecutionEntityType> entityTypes;

  @Override
  public Object bind() {
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();
    if (nodeExecutionId == null) {
      return null;
    }

    NodeExecution nodeExecution = nodeExecutionsCache.fetch(nodeExecutionId);
    if (nodeExecution == null) {
      return null;
    }

    return NodeExecutionValue.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .outcomeService(outcomeService)
        .executionSweepingOutputService(executionSweepingOutputService)
        .ambiance(ambiance)
        .startNodeExecution(nodeExecution)
        .entityTypes(entityTypes)
        .build()
        .bind();
  }
}
