package software.wings.sm;

/**
 * Describes different possible events for state.
 *
 * @author Rishi
 */
public enum ExecutionInterruptType {
  /**
   * Abort state event.
   */
  ABORT("Abort execution of the current node"),
  /**
   * Abort all state event.
   */
  ABORT_ALL("Abort execution of all nodes for the current workflow"),
  /**
   * Pause state event.
   */
  PAUSE("Pause execution of the current node"),
  /**
   * Pause all state event.
   */
  PAUSE_ALL("Pause execution of all nodes for the current workflow"),
  /**
   * Resume state event.
   */
  RESUME("Resume execution of the paused node"),
  /**
   * Resume all state event.
   */
  RESUME_ALL("Resume execution of all paused nodes in the current workflow"),
  /**
   * Retry state event.
   */
  RETRY("Retry the node execution"),
  /**
   * Ignore state event.
   */
  IGNORE("Ignore error and go to next"),
  /**
   * Mark as failed.
   */
  MARK_FAILED("Mark the node as failed"),
  /**
   * Mark as success.
   */

  MARK_SUCCESS("Mark the node as success"),

  ROLLBACK("Rollback"),

  NEXT_STEP("Next Step"),

  END_EXECUTION("End Execution"),

  ROLLBACK_DONE("Rollback Done"),

  MARK_EXPIRED("Mark the node as expired");

  private String description;

  ExecutionInterruptType(String description) {
    this.description = description;
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }
}
