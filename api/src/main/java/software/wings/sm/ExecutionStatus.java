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
  NEW, /**
        * Starting execution status.
        */
  STARTING, /**
             * Running execution status.
             */
  RUNNING, /**
            * Success execution status.
            */
  SUCCESS(true), /**
                  * Aborting execution status.
                  */
  ABORTING, /**
             * Aborted execution status.
             */
  ABORTED(true), /**
                  * Failed execution status.
                  */
  FAILED(true), /**
                 * Queued execution status.
                 */
  QUEUED, /**
           * Scheduled execution status.
           */
  SCHEDULED, /**
              * Error execution status.
              */
  ERROR(true), /**
                * Paused on error execution status.
                */
  PAUSED_ON_ERROR, /**
                    * Paused execution status.
                    */
  PAUSED;

  private boolean finalStatus = false;

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
