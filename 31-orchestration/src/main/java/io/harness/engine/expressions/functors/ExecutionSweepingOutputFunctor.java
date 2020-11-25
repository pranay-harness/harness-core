package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.expression.LateBindingMap;
import io.harness.pms.ambiance.Ambiance;
import io.harness.refObjects.RefObjectUtil;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class ExecutionSweepingOutputFunctor extends LateBindingMap {
  transient ExecutionSweepingOutputService executionSweepingOutputService;
  transient Ambiance ambiance;

  @Override
  public synchronized Object get(Object key) {
    return executionSweepingOutputService.resolve(ambiance, RefObjectUtil.getSweepingOutputRefObject((String) key));
  }
}
