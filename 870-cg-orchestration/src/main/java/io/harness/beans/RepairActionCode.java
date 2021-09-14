/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
public enum RepairActionCode {
  MANUAL_INTERVENTION,
  ROLLBACK_WORKFLOW,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  ROLLBACK_PHASE,
  IGNORE,
  RETRY,
  END_EXECUTION,
  CONTINUE_WITH_DEFAULTS,
  ABORT_WORKFLOW_EXECUTION;

  private static final Set<RepairActionCode> pipelineRuntimeInputsTimeoutAction =
      EnumSet.of(END_EXECUTION, CONTINUE_WITH_DEFAULTS);

  public static boolean isPipelineRuntimeTimeoutAction(RepairActionCode actionCode) {
    return actionCode != null && pipelineRuntimeInputsTimeoutAction.contains(actionCode);
  }
}
