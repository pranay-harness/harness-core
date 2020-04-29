package io.harness.adviser;

import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;

@Redesign
public interface Adviser extends RegistrableEntity<AdviserType> {
  AdviserType getType();
  Advise onAdviseEvent(AdvisingEvent advisingEvent);
}
