package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.LateBindingMap;
import io.harness.references.SweepingOutputRefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class ExecutionSweepingOutputFunctor extends LateBindingMap {
  transient ExecutionSweepingOutputService executionSweepingOutputService;
  transient Ambiance ambiance;

  @Override
  public synchronized Object get(Object key) {
    return executionSweepingOutputService.resolve(
        ambiance, SweepingOutputRefObject.builder().name((String) key).build());
  }
}
