package io.harness.beans.rollback;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public interface NGFailureActionTypeConstants {
  String IGNORE = "Ignore";
  String RETRY = "Retry";
  String ABORT = "Abort";
  String STAGE_ROLLBACK = "StageRollback";
  String STEP_GROUP_ROLLBACK = "StepGroupRollback";
  String MANUAL_INTERVENTION = "ManualIntervention";
  String MARK_AS_SUCCESS = "MarkAsSuccess";
}
