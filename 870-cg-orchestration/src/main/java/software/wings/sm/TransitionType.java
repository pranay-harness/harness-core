package software.wings.sm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * List of transision types between states.
 *
 * @author Rishi
 */
@OwnedBy(HarnessTeam.CDC)
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
