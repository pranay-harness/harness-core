package software.wings.sm;

/**
 * Describes different possible events for state.
 *
 * @author Rishi
 */
public enum StateEvent {
  /**
   * Success state event.
   */
  SUCCESS, /**
            * Failure state event.
            */
  FAILURE, /**
            * Abort state event.
            */
  ABORT, /**
          * Pause state event.
          */
  PAUSE, /**
          * Continue state event.
          */
  CONTINUE, /**
             * Retry state event.
             */
  RETRY;
}
