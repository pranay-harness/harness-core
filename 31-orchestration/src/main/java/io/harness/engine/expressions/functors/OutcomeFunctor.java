package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.LateBindingMap;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class OutcomeFunctor extends LateBindingMap {
  transient OutcomeService outcomeService;
  transient Ambiance ambiance;

  public synchronized Object output(String name) {
    return outcomeService.findOutcome(ambiance, name);
  }

  @Override
  public synchronized Object get(Object key) {
    return output((String) key);
  }
}
