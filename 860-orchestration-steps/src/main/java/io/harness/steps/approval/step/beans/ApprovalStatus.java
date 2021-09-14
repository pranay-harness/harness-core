/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.approval.step.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnsupportedOperationException;
import io.harness.pms.contracts.execution.Status;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public enum ApprovalStatus {
  WAITING,
  APPROVED,
  REJECTED,
  FAILED,
  EXPIRED;

  // FINAL_STATUSES is a list of statuses which when set change the orchestration status of the step also.
  // NOTE: EXPIRED is not a final status as handling of expired steps is done separately by pipeline service
  // independent of the approval instance status. The EXPIRED status is there just to ensure we don't keep on
  // iterating on instances which have expired.
  private static final Set<ApprovalStatus> FINAL_STATUSES = EnumSet.of(APPROVED, REJECTED, FAILED);

  public boolean isFinalStatus() {
    return FINAL_STATUSES.contains(this);
  }

  public Status toFinalExecutionStatus() {
    switch (this) {
      case APPROVED:
        return Status.SUCCEEDED;
      case REJECTED:
        return Status.APPROVAL_REJECTED;
      case FAILED:
        return Status.FAILED;
      default:
        throw new UnsupportedOperationException(String.format("Invalid status: %s", name()));
    }
  }
}
