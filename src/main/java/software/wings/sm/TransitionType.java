package software.wings.sm;

/**
 * List of transision types between states.
 *
 * @author Rishi
 */
public enum TransitionType {
  SUCCESS,
  FAILURE,
  ABORT,
  REPEAT,
  FORK,
  CONDITIONAL;
}
