package io.harness.limits.configuration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;

@OwnedBy(PL)
public class NoLimitConfiguredException extends RuntimeException {
  public NoLimitConfiguredException(Action action) {
    super("No limit configured. Action: " + action);
  }
}
