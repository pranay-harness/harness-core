package io.harness.state.execution.status;

import io.harness.annotations.Redesign;

// TODO (prashant) : Do we need this class, better to separate out the statuses for node and execution instance?
@Redesign
public enum ExecutionInstanceStatus {
  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  WAITING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  PAUSED,
  RESUMED,
  REJECTED,
  ABORTED,
  SUCCEEDED,
  FAILED,
  ERRORED
}
