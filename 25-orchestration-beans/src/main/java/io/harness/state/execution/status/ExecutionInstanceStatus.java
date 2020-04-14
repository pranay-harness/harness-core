package io.harness.state.execution.status;

import io.harness.annotations.Redesign;

// TODO (prashant) : Do we need this class, better to separate out the statuses for node and execution instance?
@Redesign
public enum ExecutionInstanceStatus {

  RUNNING,
  WAITING,

  QUEUED,
  PAUSED,
  RESUMED,
  REJECTED,
  ABORTED,
  SUCCEEDED,
  FAILED,
  ERRORED
}
