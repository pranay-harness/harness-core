package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.LateBindingValue;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionQualifiedFunctor implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  OutcomeService outcomeService;
  ExecutionSweepingOutputService executionSweepingOutputService;
  Ambiance ambiance;

  @Override
  public Object bind() {
    return NodeExecutionValue.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .outcomeService(outcomeService)
        .executionSweepingOutputService(executionSweepingOutputService)
        .ambiance(ambiance)
        .startNodeExecution(null)
        .build()
        .bind();
  }
}
