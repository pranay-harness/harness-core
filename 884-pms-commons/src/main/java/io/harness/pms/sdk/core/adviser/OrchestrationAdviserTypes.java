package io.harness.pms.sdk.core.adviser;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
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
