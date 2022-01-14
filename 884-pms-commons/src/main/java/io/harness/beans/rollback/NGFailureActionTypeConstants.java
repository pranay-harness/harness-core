package io.harness.beans.rollback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NGFailureActionTypeConstants {
  String IGNORE = "Ignore";
  String RETRY = "Retry";
  String ABORT = "Abort";
  String STAGE_ROLLBACK = "StageRollback";
  String STEP_GROUP_ROLLBACK = "StepGroupRollback";
  String MANUAL_INTERVENTION = "ManualIntervention";
  String MARK_AS_SUCCESS = "MarkAsSuccess";
}
