package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public enum NodeExecutionStatus {

  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  TASK_WAITING,
  CHILD_WAITING,
  CHILDREN_WAITING,
  TIMED_WAITING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  ERRORED,
  FAILED,
  SUCCEEDED

}
