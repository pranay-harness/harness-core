package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TimeoutParameters {
  Long DEFAULT_TIMEOUT_IN_MILLIS = 10_000L;
  long getTimeoutMillis();
}
