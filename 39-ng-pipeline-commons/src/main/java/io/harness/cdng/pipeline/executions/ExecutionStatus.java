package io.harness.cdng.pipeline.executions;

import com.google.common.collect.Sets;

import io.harness.execution.status.Status;

import java.util.Set;

public enum ExecutionStatus {
  RUNNING(Sets.newHashSet(Status.RUNNING)),
  FAILED(Sets.newHashSet(Status.ERRORED, Status.FAILED)),
  NOT_STARTED(Sets.newHashSet()),
  EXPIRED(Sets.newHashSet(Status.EXPIRED)),
  ABORTED(Sets.newHashSet(Status.ABORTED)),
  QUEUED(Sets.newHashSet(Status.QUEUED)),
  PAUSED(Sets.newHashSet(Status.PAUSED)),
  WAITING(
      Sets.newHashSet(Status.ASYNC_WAITING, Status.TASK_WAITING, Status.INTERVENTION_WAITING, Status.TIMED_WAITING)),
  SUCCESS(Sets.newHashSet(Status.SUCCEEDED)),
  SUSPENDED(Sets.newHashSet(Status.SUSPENDED));

  Set<Status> engineStatuses;

  ExecutionStatus(Set<Status> engineStatuses) {
    this.engineStatuses = engineStatuses;
  }
  static final Set<ExecutionStatus> terminalStatuses = Sets.newHashSet(FAILED, SUCCESS);

  public static boolean isTerminal(ExecutionStatus executionStatus) {
    return terminalStatuses.contains(executionStatus);
  }

  public static ExecutionStatus getExecutionStatus(Status status) {
    for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
      if (executionStatus.engineStatuses.contains(status)) {
        return executionStatus;
      }
    }
    throw new IllegalArgumentException(String.format("No Execution Status mapper found for input status: %s", status));
  }
}
