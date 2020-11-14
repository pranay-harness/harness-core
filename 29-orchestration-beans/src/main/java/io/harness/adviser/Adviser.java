package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public interface Adviser<T extends AdviserParameters> {
  @NotNull Advise onAdviseEvent(AdvisingEvent<T> advisingEvent);

  boolean canAdvise(AdvisingEvent<T> advisingEvent);
}
