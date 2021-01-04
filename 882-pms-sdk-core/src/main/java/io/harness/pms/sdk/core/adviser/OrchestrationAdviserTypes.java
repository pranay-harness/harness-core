package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum OrchestrationAdviserTypes {
  // Provided From the orchestration layer system advisers

  // SUCCESS
  ON_SUCCESS,

  // FAILURES
  ON_FAIL,
  IGNORE,
  RETRY,

  ABORT,
  PAUSE,
  RESUME,
  MANUAL_INTERVENTION,

  MARK_SUCCESS
}
