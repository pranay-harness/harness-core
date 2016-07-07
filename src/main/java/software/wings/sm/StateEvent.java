package software.wings.sm;

/**
 * Describes different possible events for state.
 *
 * @author Rishi
 */
public enum StateEvent {
  /**
   * Abort state event.
   */
  ABORT,
  /**
   * Abort all state event.
   */
  ABORT_ALL,
  /**
   * Pause state event.
   */
  PAUSE,
  /**
   * Pause all state event.
   */
  PAUSE_ALL,
  /**
   * Continue state event.
   */
  CONTINUE,
  /**
   * Retry state event.
   */
  RETRY;
}
