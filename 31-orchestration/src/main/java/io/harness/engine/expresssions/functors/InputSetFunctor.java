package io.harness.engine.expresssions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.LateBindingMap;
import io.harness.plan.input.InputSet;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class InputSetFunctor extends LateBindingMap {
  transient InputSet inputSet;

  @Override
  public synchronized Object get(Object key) {
    return inputSet == null ? null : inputSet.get((String) key);
  }
}
