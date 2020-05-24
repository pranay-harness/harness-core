package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

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

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  ERRORED,
  FAILED,
  SUCCEEDED;

  private static final Set<NodeExecutionStatus> POSITIVE_STATUSES = EnumSet.<NodeExecutionStatus>of(SUCCEEDED, SKIPPED);

  private static final Set<NodeExecutionStatus> BROKE_STATUSES = EnumSet.<NodeExecutionStatus>of(FAILED, ERRORED);

  public static Set<NodeExecutionStatus> positiveStatuses() {
    return POSITIVE_STATUSES;
  }

  public static Set<NodeExecutionStatus> brokeStatuses() {
    return BROKE_STATUSES;
  }
}
