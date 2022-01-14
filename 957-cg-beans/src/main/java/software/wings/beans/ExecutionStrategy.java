/**
 *
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * The enum Execution strategy.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public enum ExecutionStrategy {
  /**
   * Serial execution strategy.
   */
  SERIAL,
  /**
   * Parallel execution strategy.
   */
  PARALLEL;
}
