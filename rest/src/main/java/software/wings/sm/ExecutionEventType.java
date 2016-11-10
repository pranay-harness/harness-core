package software.wings.sm;

/**
 * Describes different possible events for state.
 *
 * @author Rishi
 */
public enum ExecutionEventType {
  /**
   * Abort state event.
   */
  ABORT("Abort execution of the current node"), /**
                                                 * Abort all state event.
                                                 */
  ABORT_ALL("Abort execution of all nodes for the current workflow"), /**
                                                                       * Pause state event.
                                                                       */
  PAUSE("Pause execution of the current node"), /**
                                                 * Pause all state event.
                                                 */
  PAUSE_ALL("Pause execution of all nodes for the current workflow"), /**
                                                                       * Resume state event.
                                                                       */
  RESUME("Resume execution of the paused node"), /**
                                                  * Resume all state event.
                                                  */
  RESUME_ALL("Resume execution of all paused nodes in the current workflow"), /**
                                                                               * Retry state event.
                                                                               */
  RETRY("Retry the node execution"), /**
                                      * Ignore state event.
                                      */
  IGNORE("Ignore error and go to next"), /**
                                          * Mark fixed state event.
                                          */
  MARK_SUCCESS("Mark the node as success and go to next");

  private String description;

  private ExecutionEventType(String description) {
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
