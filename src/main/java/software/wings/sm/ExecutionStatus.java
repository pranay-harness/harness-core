package software.wings.sm;

/**
 * Describes possible execution statuses for a state.
 *
 * @author Rishi
 */
public enum ExecutionStatus {
  NEW,
  RUNNING,
  SUCCESS,
  ABORTED,
  FAILED,
  ERROR;
}
