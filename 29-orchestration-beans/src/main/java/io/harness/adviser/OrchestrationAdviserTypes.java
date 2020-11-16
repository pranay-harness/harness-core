package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
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
  MANUAL_INTERVENTION;
}
