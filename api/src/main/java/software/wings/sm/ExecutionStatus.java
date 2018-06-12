package software.wings.sm;

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
  private static Set<ExecutionStatus> activeStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, QUEUED, WAITING);
  private static Set<ExecutionStatus> positiveStatuses = EnumSet.<ExecutionStatus>of(SUCCESS, SKIPPED);
  private static Set<ExecutionStatus> discontinueStatuses = EnumSet.<ExecutionStatus>of(ABORTED, REJECTED, EXPIRED);

  ExecutionStatus() {}

  public static boolean isFinalStatus(ExecutionStatus status) {
    return status != null && finalStatuses.contains(status);
  }

  public static Set<ExecutionStatus> brokeStatuses() {
    return brokeStatuses;
  }

  public static boolean isBrokeStatus(ExecutionStatus status) {
    return status != null && brokeStatuses.contains(status);
  }

  public static boolean isNegativeStatus(ExecutionStatus status) {
    return status != null && negativeStatuses.contains(status);
  }

  public static boolean isRunningStatus(ExecutionStatus status) {
    return status != null && runningStatuses.contains(status);
  }

  public static boolean isDiscontinueStatus(ExecutionStatus status) {
    return status != null && discontinueStatuses.contains(status);
  }

  public static Set<ExecutionStatus> activeStatuses() {
    return activeStatuses;
  }

  public static Set<ExecutionStatus> negativeStatuses() {
    return negativeStatuses;
  }

  public static boolean isPositiveStatus(ExecutionStatus status) {
    return status != null && positiveStatuses.contains(status);
  }
}
