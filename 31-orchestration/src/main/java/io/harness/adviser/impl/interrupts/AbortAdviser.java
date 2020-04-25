package io.harness.adviser.impl.interrupts;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class AbortAdviser implements Adviser {
  AdviserType type = AdviserType.builder().type(AdviserType.ABORT).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return null;
  }
}
