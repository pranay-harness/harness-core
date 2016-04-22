package software.wings.sm;

/**
 * Describes different possible events for state.
 * @author Rishi
 */
public enum StateEvent {
  SUCCESS,
  FAILURE,
  ABORT,
  PAUSE,
  CONTINUE,
  RETRY;
}
