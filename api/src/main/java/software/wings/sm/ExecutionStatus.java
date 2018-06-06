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
  WAITING;

  private static Set<ExecutionStatus> finalStatuses = EnumSet.<ExecutionStatus>of(ABORTED, ERROR, FAILED, SUCCESS);
  private static Set<ExecutionStatus> brokeStatuses = EnumSet.<ExecutionStatus>of(ERROR, FAILED);
  private static Set<ExecutionStatus> failStatuses = EnumSet.<ExecutionStatus>of(ABORTED, DISCONTINUING, ERROR, FAILED);
  private static Set<ExecutionStatus> runningStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, RUNNING, STARTING, QUEUED);
  private static Set<ExecutionStatus> activeStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, QUEUED, WAITING);

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

  public static boolean isFailStatus(ExecutionStatus status) {
    return status != null && failStatuses.contains(status);
  }

  public static boolean isRunningStatus(ExecutionStatus status) {
    return status != null && runningStatuses.contains(status);
  }

  public static Set<ExecutionStatus> activeStatuses() {
    return activeStatuses;
  }

  public static Set<ExecutionStatus> failStatuses() {
    return failStatuses;
  }

  public static boolean isSuccessStatus(ExecutionStatus status) {
    return status != null && status == SUCCESS;
  }
}
