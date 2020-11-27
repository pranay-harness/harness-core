package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingValue;
import io.harness.pms.ambiance.Ambiance;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

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
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
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
