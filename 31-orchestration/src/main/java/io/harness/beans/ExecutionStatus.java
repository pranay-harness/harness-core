package io.harness.beans;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * Describes possible execution statuses for a state.
 */
public enum ExecutionStatus {
  ABORTED,
  DISCONTINUING,
  ERROR,
  FAILED,
  NEW,
  PAUSED,
  PAUSING,
  QUEUED,
  RESUMED,
  RUNNING,
  SCHEDULED,
  STARTING,
  SUCCESS,
  WAITING,
  SKIPPED,
  @Deprecated ABORTING,
  REJECTED,
  EXPIRED;

  private static Set<ExecutionStatus> finalStatuses =
      EnumSet.<ExecutionStatus>of(ABORTED, ERROR, FAILED, SUCCESS, REJECTED, EXPIRED, SKIPPED);
  private static Set<ExecutionStatus> brokeStatuses = EnumSet.<ExecutionStatus>of(ERROR, FAILED);
  private static Set<ExecutionStatus> negativeStatuses =
      EnumSet.<ExecutionStatus>of(ABORTED, DISCONTINUING, ERROR, FAILED, REJECTED, EXPIRED);
  private static Set<ExecutionStatus> runningStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, RUNNING, STARTING, QUEUED);
  private static Set<ExecutionStatus> haltedStatuses = EnumSet.<ExecutionStatus>of(PAUSED, WAITING);
  private static Set<ExecutionStatus> activeStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, QUEUED, WAITING);
  private static Set<ExecutionStatus> positiveStatuses = EnumSet.<ExecutionStatus>of(SUCCESS, SKIPPED);
  private static Set<ExecutionStatus> discontinueStatuses = EnumSet.<ExecutionStatus>of(ABORTED, REJECTED, EXPIRED);
  private static Set<ExecutionStatus> flowingStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, WAITING);

  ExecutionStatus() {}

  public static Set<ExecutionStatus> activeStatuses() {
    return activeStatuses;
  }

  public static Set<ExecutionStatus> finalStatuses() {
    return finalStatuses;
  }

  public static boolean isFinalStatus(ExecutionStatus status) {
    return status != null && finalStatuses.contains(status);
  }

  public static Set<ExecutionStatus> brokeStatuses() {
    return brokeStatuses;
  }

  public static boolean isBrokeStatus(ExecutionStatus status) {
    return status != null && brokeStatuses.contains(status);
  }

  public static Set<ExecutionStatus> negativeStatuses() {
    return negativeStatuses;
  }

  public static Set<ExecutionStatus> flowingStatuses() {
    return flowingStatuses;
  }

  public static boolean isNegativeStatus(ExecutionStatus status) {
    return status != null && negativeStatuses.contains(status);
  }

  public static Set<ExecutionStatus> runningStatuses() {
    return runningStatuses;
  }

  public static boolean isRunningStatus(ExecutionStatus status) {
    return status != null && runningStatuses.contains(status);
  }

  public static boolean isHaltedStatus(ExecutionStatus status) {
    return status != null && haltedStatuses.contains(status);
  }

  public static boolean isDiscontinueStatus(ExecutionStatus status) {
    return status != null && discontinueStatuses.contains(status);
  }

  public static boolean isPositiveStatus(ExecutionStatus status) {
    return status != null && positiveStatuses.contains(status);
  }

  public static CommandExecutionStatus translateExecutionStatus(ExecutionStatus executionStatus) {
    switch (executionStatus) {
      case SUCCESS:
        return CommandExecutionStatus.SUCCESS;
      case FAILED:
        return CommandExecutionStatus.FAILURE;
      case RUNNING:
        return CommandExecutionStatus.RUNNING;
      case QUEUED:
        return CommandExecutionStatus.QUEUED;
      default:
        throw new IllegalArgumentException("invalid status: " + executionStatus);
    }
  }

  public static ExecutionStatusCategory getStatusCategory(ExecutionStatus status) {
    if (ExecutionStatus.isPositiveStatus(status)) {
      return ExecutionStatusCategory.SUCCEEDED;
    } else if (ExecutionStatus.isNegativeStatus(status)) {
      return ExecutionStatusCategory.ERROR;
    } else {
      return ExecutionStatusCategory.ACTIVE;
    }
  }
}
