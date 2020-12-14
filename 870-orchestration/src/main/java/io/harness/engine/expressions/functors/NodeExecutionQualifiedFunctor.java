package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.expression.LateBindingValue;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionQualifiedFunctor implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  OutcomeService outcomeService;
  PmsSweepingOutputService pmsSweepingOutputService;
  Ambiance ambiance;
  Set<NodeExecutionEntityType> entityTypes;

  @Override
  public Object bind() {
    return NodeExecutionValue.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .outcomeService(outcomeService)
        .pmsSweepingOutputService(pmsSweepingOutputService)
        .ambiance(ambiance)
        .startNodeExecution(null)
        .entityTypes(entityTypes)
        .build()
        .bind();
  }
}
