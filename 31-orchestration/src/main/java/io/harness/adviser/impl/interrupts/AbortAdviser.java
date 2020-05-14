package io.harness.adviser.impl.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public class AbortAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.ABORT).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return null;
  }

  @Override
  public AdviserType getType() {
    return ADVISER_TYPE;
  }
}
