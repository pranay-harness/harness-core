package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

@OwnedBy(CDC)
public enum GcbBuildStatus {
  STATUS_UNKNOWN(ExecutionStatus.FAILED),
  QUEUED(ExecutionStatus.QUEUED),
  WORKING(ExecutionStatus.RUNNING),
  SUCCESS(ExecutionStatus.SUCCESS),
  FAILURE(ExecutionStatus.FAILED),
  INTERNAL_ERROR(ExecutionStatus.ERROR),
  TIMEOUT(ExecutionStatus.EXPIRED),
  CANCELLED(ExecutionStatus.ABORTED),
  EXPIRED(ExecutionStatus.EXPIRED);

  private final ExecutionStatus status;

  GcbBuildStatus(ExecutionStatus status) {
    this.status = status;
  }

  public ExecutionStatus getExecutionStatus() {
    return status;
  }
}
