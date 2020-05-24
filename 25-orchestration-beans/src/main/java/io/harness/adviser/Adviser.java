package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(CDC)
@Redesign
public interface Adviser extends RegistrableEntity<AdviserType> {
  AdviserType getType();

  Advise onAdviseEvent(AdvisingEvent advisingEvent);
}
