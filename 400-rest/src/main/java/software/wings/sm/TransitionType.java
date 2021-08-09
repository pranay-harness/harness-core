package software.wings.sm;

/**
 * List of transision types between states.
 *
 * @author Rishi
 */
public enum TransitionType {
  /**
   * Success transition type.
   */
  SUCCESS,
  /**
   * Failure transition type.
   */
  FAILURE,
  /**
   * Abort transition type.
   */
  ABORT,
  /**
   * Repeat transition type.
   */
  REPEAT,
  /**
   * Fork transition type.
   */
  FORK,
  /**
   * Conditional transition type.
   */
  CONDITIONAL;
}
