/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.rollback;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._870_YAML_BEANS)
public interface NGFailureActionTypeConstants {
  String IGNORE = "Ignore";
  String RETRY = "Retry";
  String ABORT = "Abort";
  String STAGE_ROLLBACK = "StageRollback";
  String STEP_GROUP_ROLLBACK = "StepGroupRollback";
  String MANUAL_INTERVENTION = "ManualIntervention";
  String MARK_AS_SUCCESS = "MarkAsSuccess";
}
