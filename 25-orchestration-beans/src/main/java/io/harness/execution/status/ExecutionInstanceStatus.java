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
