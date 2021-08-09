package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface StateConstants {
  /**
   * The constant DEFAULT_STEADY_STATE_TIMEOUT.
   */
  int DEFAULT_STEADY_STATE_TIMEOUT = 10;
}
