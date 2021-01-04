package io.harness.pms.execution;

import io.harness.pms.contracts.execution.Status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum ExecutionStatus {
  @JsonProperty("Running")
  RUNNING(Sets.newHashSet(Status.RUNNING, Status.ASYNC_WAITING, Status.TASK_WAITING, Status.TIMED_WAITING), "Running"),
  @JsonProperty("Failed") FAILED(Sets.newHashSet(Status.ERRORED, Status.FAILED), "Failed"),
  @JsonProperty("NotStarted") NOT_STARTED(Sets.newHashSet(), "NotStarted"),
  @JsonProperty("Expired") EXPIRED(Sets.newHashSet(Status.EXPIRED), "Expired"),
  @JsonProperty("Aborted") ABORTED(Sets.newHashSet(Status.ABORTED, Status.DISCONTINUING), "Aborted"),
  @JsonProperty("Queued") QUEUED(Sets.newHashSet(Status.QUEUED), "Queued"),
  @JsonProperty("Paused") PAUSED(Sets.newHashSet(Status.PAUSED), "Paused"),
  @JsonProperty("Waiting") WAITING(Sets.newHashSet(Status.INTERVENTION_WAITING), "Waiting"),
  @JsonProperty("Success") SUCCESS(Sets.newHashSet(Status.SUCCEEDED), "Success"),
  @JsonProperty("Suspended") SUSPENDED(Sets.newHashSet(Status.SUSPENDED), "Suspended");

  static final Set<ExecutionStatus> TERMINAL_STATUSES = Sets.newHashSet(FAILED, SUCCESS, ABORTED, EXPIRED);
  public static final Set<Status> BROKE_STATUSES = EnumSet.of(Status.FAILED, Status.ERRORED);

  Set<Status> engineStatuses;
  String displayName;

  ExecutionStatus(Set<Status> engineStatuses, String displayName) {
    this.engineStatuses = engineStatuses;
    this.displayName = displayName;
  }

  @JsonCreator
  public static ExecutionStatus getExecutionStatus(@JsonProperty("type") String displayName) {
    for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
      if (executionStatus.displayName.equalsIgnoreCase(displayName)) {
        return executionStatus;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(ExecutionStatus.values())));
  }

  public static boolean isTerminal(ExecutionStatus executionStatus) {
    return TERMINAL_STATUSES.contains(executionStatus);
  }

  public static ExecutionStatus getExecutionStatus(Status status) {
    for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
      if (executionStatus.engineStatuses.contains(status)) {
        return executionStatus;
      }
    }
    throw new IllegalArgumentException(String.format("No Execution Status mapper found for input status: %s", status));
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
