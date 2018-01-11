package software.wings.sm;

/**
 * Describes possible execution statuses for a state.
 *
 * @author Rishi
 */
public enum ExecutionStatus {
  /**
   * New execution status.
   */
  NEW,
  /**
   * Starting execution status.
   */
  STARTING,
  /**
   * Running execution status.
   */
  RUNNING,
  /**
   * Success execution status.
   */
  SUCCESS(true),
  /**
   * Aborting execution status.
   */
  ABORTING,
  /**
   * Aborted execution status.
   */
  ABORTED(true),
  /**
   * Failed execution status.
   */
  FAILED(true),
  /**
   * Queued execution status.
   */
  QUEUED,
  /**
   * Scheduled execution status.
   */
  SCHEDULED,
  /**
   * Error execution status.
   */
  ERROR(true),
  /**
   * Waiting on error execution status.
   */
  WAITING,
  /**
   * Pausing on execution
   */
  PAUSING,
  /**
   * Paused execution status.
   */
  PAUSED,
  /**
   * Resumed execution status.
   */
  RESUMED;

  private boolean finalStatus;

  ExecutionStatus() {}

  ExecutionStatus(boolean finalStatus) {
    this.finalStatus = finalStatus;
  }

  /**
   * Is final status boolean.
   *
   * @return the boolean
   */
  public boolean isFinalStatus() {
    return finalStatus;
  }
}
