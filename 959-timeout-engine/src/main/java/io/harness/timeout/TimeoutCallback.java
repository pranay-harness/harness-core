package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TimeoutCallback {
  void onTimeout(TimeoutInstance timeoutInstance);
}
