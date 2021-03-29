package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface Limit {
  /**
   * Type of limit. Static, or rate based.
   */
  LimitType getLimitType();
}
