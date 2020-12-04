package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserResponse;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public interface Adviser {
  @NotNull AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent);

  boolean canAdvise(AdvisingEvent advisingEvent);
}
