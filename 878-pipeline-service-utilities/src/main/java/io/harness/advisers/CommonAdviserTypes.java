package io.harness.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum CommonAdviserTypes {
  RETRY_WITH_ROLLBACK,
  MANUAL_INTERVENTION_WITH_ROLLBACK,
  ON_FAIL_ROLLBACK
}
