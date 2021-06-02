package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.contracts.execution.Status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@TargetModule(HarnessModule._888_PMS_CLIENT)
@OwnedBy(PIPELINE)
public enum ExecutionStatus {
  @JsonProperty("Running")
  RUNNING(Sets.newHashSet(Status.RUNNING, Status.ASYNC_WAITING, Status.TASK_WAITING, Status.TIMED_WAITING), "Running"),
  @JsonProperty("Failed") FAILED(Sets.newHashSet(Status.ERRORED, Status.FAILED, Status.IGNORE_FAILED), "Failed"),
  @JsonProperty("NotStarted") NOTSTARTED(Sets.newHashSet(), "NotStarted"),
  @JsonProperty("Expired") EXPIRED(Sets.newHashSet(Status.EXPIRED), "Expired"),
  @JsonProperty("Aborted") ABORTED(Sets.newHashSet(Status.ABORTED, Status.DISCONTINUING), "Aborted"),
  @JsonProperty("Queued") QUEUED(Sets.newHashSet(Status.QUEUED), "Queued"),
  @JsonProperty("Paused") PAUSED(Sets.newHashSet(Status.PAUSED), "Paused"),
  @JsonProperty("Waiting") WAITING(Sets.newHashSet(Status.RESOURCE_WAITING), "Waiting"),
  @JsonProperty("InterventionWaiting")
  INTERVENTIONWAITING(Sets.newHashSet(Status.INTERVENTION_WAITING), "InterventionWaiting"),
  @JsonProperty("ApprovalWaiting") APPROVALWAITING(Sets.newHashSet(Status.APPROVAL_WAITING), "ApprovalWaiting"),
  @JsonProperty("Success") SUCCESS(Sets.newHashSet(Status.SUCCEEDED), "Success"),
  @JsonProperty("Suspended") SUSPENDED(Sets.newHashSet(Status.SUSPENDED), "Suspended"),
  @JsonProperty("Skipped") SKIPPED(Sets.newHashSet(Status.SKIPPED), "Skipped"),
  @JsonProperty("Pausing") PAUSING(Sets.newHashSet(Status.PAUSING), "Pausing"),
  @JsonProperty("ApprovalRejected") APPROVALREJECTED(Sets.newHashSet(Status.APPROVAL_REJECTED), "ApprovalRejected"),

  //@JsonIgnore added to not show older enums till migration is written to change their instances to new enums in DB.
  @JsonIgnore NOT_STARTED(Sets.newHashSet()),
  @JsonIgnore INTERVENTION_WAITING(Sets.newHashSet(Status.INTERVENTION_WAITING)),
  @JsonIgnore APPROVAL_WAITING(Sets.newHashSet(Status.APPROVAL_WAITING)),
  @JsonIgnore APPROVAL_REJECTED(Sets.newHashSet(Status.APPROVAL_REJECTED));

  private static final Set<ExecutionStatus> TERMINAL_STATUSES =
      Sets.newHashSet(FAILED, SUCCESS, ABORTED, EXPIRED, APPROVALREJECTED);
  public static final Set<Status> BROKE_STATUSES = EnumSet.of(Status.FAILED, Status.ERRORED, Status.APPROVAL_REJECTED);

  Set<Status> engineStatuses;
  String displayName;

  ExecutionStatus(Set<Status> engineStatuses, String displayName) {
    this.engineStatuses = engineStatuses;
    this.displayName = displayName;
  }

  // Made for JsonIgnore enums. To be removed once migration code is written.
  ExecutionStatus(Set<Status> engineStatuses) {
    this.engineStatuses = engineStatuses;
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
