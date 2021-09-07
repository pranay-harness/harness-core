package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;

@OwnedBy(CDC)
public interface TimeoutParameters {
  Long DEFAULT_TIMEOUT_IN_MILLIS = Duration.ofHours(10).toMillis();
  String DEFAULT_TIMEOUT_STRING = "10h";
  long getTimeoutMillis();
}
