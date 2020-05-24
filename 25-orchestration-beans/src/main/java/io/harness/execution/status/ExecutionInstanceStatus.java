package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

// TODO (prashant) : Do we need this class, better to separate out the statuses for node and execution instance?
@OwnedBy(CDC)
@Redesign
public enum ExecutionInstanceStatus {
  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  WAITING,
  PAUSING,
  DISCONTINUING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  PAUSED,
  RESUMED,
  REJECTED,
  ABORTED,
  SUCCEEDED,
  FAILED,
  ERRORED;

  public static ExecutionInstanceStatus obtainForNodeExecutionStatus(NodeExecutionStatus nodeExecutionStatus) {
    switch (nodeExecutionStatus) {
      case RUNNING:
      case ASYNC_WAITING:
      case TASK_WAITING:
      case CHILD_WAITING:
      case CHILDREN_WAITING:
        return RUNNING;
      case TIMED_WAITING:
        return WAITING;
      case DISCONTINUING:
        return DISCONTINUING;
      case PAUSING:
        return PAUSING;
      case QUEUED:
        return QUEUED;
      case PAUSED:
        return PAUSED;
      case ABORTED:
        return ABORTED;
      case ERRORED:
        return ERRORED;
      case FAILED:
        return FAILED;
      case SUCCEEDED:
        return SUCCEEDED;
      default:
        throw new IllegalStateException("Unexpected value: " + nodeExecutionStatus);
    }
  }
}
