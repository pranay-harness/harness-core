package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.logging.CommandExecutionStatus;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum GcbBuildStatus {
  STATUS_UNKNOWN(ExecutionStatus.FAILED, CommandExecutionStatus.FAILURE),
  QUEUED(ExecutionStatus.QUEUED, CommandExecutionStatus.QUEUED),
  WORKING(ExecutionStatus.RUNNING, CommandExecutionStatus.RUNNING),
  SUCCESS(ExecutionStatus.SUCCESS, CommandExecutionStatus.SUCCESS),
  FAILURE(ExecutionStatus.FAILED, CommandExecutionStatus.FAILURE),
  INTERNAL_ERROR(ExecutionStatus.ERROR, CommandExecutionStatus.FAILURE),
  TIMEOUT(ExecutionStatus.EXPIRED, CommandExecutionStatus.FAILURE),
  CANCELLED(ExecutionStatus.ABORTED, CommandExecutionStatus.FAILURE),
  EXPIRED(ExecutionStatus.EXPIRED, CommandExecutionStatus.FAILURE);

  private final ExecutionStatus status;
  private final CommandExecutionStatus commandExecutionStatus;

  GcbBuildStatus(ExecutionStatus status, CommandExecutionStatus commandExecutionStatus) {
    this.status = status;
    this.commandExecutionStatus = commandExecutionStatus;
  }

  public ExecutionStatus getExecutionStatus() {
    return status;
  }
  public CommandExecutionStatus getCommandExecutionStatus() {
    return commandExecutionStatus;
  }
}
