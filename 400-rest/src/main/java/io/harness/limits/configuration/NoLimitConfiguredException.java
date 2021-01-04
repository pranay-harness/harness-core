package io.harness.limits.configuration;

import io.harness.limits.Action;

public class NoLimitConfiguredException extends RuntimeException {
  public NoLimitConfiguredException(Action action) {
    super("No limit configured. Action: " + action);
  }
}
