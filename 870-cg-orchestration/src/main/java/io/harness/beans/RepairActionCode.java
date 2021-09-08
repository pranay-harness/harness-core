package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
public enum RepairActionCode {
  MANUAL_INTERVENTION,
  ROLLBACK_WORKFLOW,
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
