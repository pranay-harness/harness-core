package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserResponse;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface Adviser {
  @NotNull AdviserResponse onAdviseEvent(io.harness.pms.sdk.core.adviser.AdvisingEvent advisingEvent);

  boolean canAdvise(AdvisingEvent advisingEvent);
}
