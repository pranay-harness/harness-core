package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(CDC)
@Redesign
public enum NodeExecutionStatus {
  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  ASYNC_WAITING,
  TASK_WAITING,
  CHILD_WAITING,
  CHILDREN_WAITING,
  TIMED_WAITING,

  DISCONTINUING,
  PAUSING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  PAUSED,
  ABORTED,
  ERRORED,
  FAILED,
  SUCCEEDED;

  // Status Groups
  private static final EnumSet<NodeExecutionStatus> ABORTABLE_STATUSES = EnumSet.of(
      QUEUED, RUNNING, PAUSED, PAUSING, ASYNC_WAITING, TASK_WAITING, CHILD_WAITING, CHILDREN_WAITING, TIMED_WAITING);

  private static final EnumSet<NodeExecutionStatus> POSITIVE_STATUSES = EnumSet.of(SUCCEEDED, SKIPPED);

  private static final EnumSet<NodeExecutionStatus> BROKE_STATUSES = EnumSet.of(FAILED, ERRORED);

  private static final EnumSet<NodeExecutionStatus> RESUMABLE_STATUSES =
      EnumSet.of(ASYNC_WAITING, CHILD_WAITING, CHILDREN_WAITING, TIMED_WAITING, TASK_WAITING);

  public static EnumSet<NodeExecutionStatus> abortableStatuses() {
    return ABORTABLE_STATUSES;
  }

  public static EnumSet<NodeExecutionStatus> positiveStatuses() {
    return POSITIVE_STATUSES;
  }

  public static EnumSet<NodeExecutionStatus> brokeStatuses() {
    return BROKE_STATUSES;
  }

  public static EnumSet<NodeExecutionStatus> resumableStatuses() {
    return RESUMABLE_STATUSES;
  }
}
