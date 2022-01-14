package io.harness.pms.sdk.core.adviser;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public enum OrchestrationAdviserTypes {
  // Provided From the orchestration layer system advisers

  // SUCCESS
  ON_SUCCESS,

  // NEXT_STEP
  NEXT_STEP,

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
