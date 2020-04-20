package io.harness.adviser;

import io.harness.annotations.Redesign;

@Redesign
public interface Adviser {
  AdviserType getType();
  Advise onAdviseEvent(AdvisingEvent advisingEvent);
}
