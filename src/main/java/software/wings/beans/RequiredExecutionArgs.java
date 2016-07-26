/**
 *
 */

package software.wings.beans;

import java.util.HashSet;
import java.util.Set;

/**
 * The type Required execution args.
 *
 * @author Rishi
 */
public class RequiredExecutionArgs {
  private Set<ExecutionArgumentType> requiredExecutionTypes = new HashSet<>();
  private ExecutionArgs defaultExecutionArgs = new ExecutionArgs();

  /**
   * Gets required execution types.
   *
   * @return the required execution types
   */
  public Set<ExecutionArgumentType> getRequiredExecutionTypes() {
    return requiredExecutionTypes;
  }

  /**
   * Sets required execution types.
   *
   * @param requiredExecutionTypes the required execution types
   */
  public void setRequiredExecutionTypes(Set<ExecutionArgumentType> requiredExecutionTypes) {
    this.requiredExecutionTypes = requiredExecutionTypes;
  }

  /**
   * Gets default execution args.
   *
   * @return the default execution args
   */
  public ExecutionArgs getDefaultExecutionArgs() {
    return defaultExecutionArgs;
  }

  /**
   * Sets default execution args.
   *
   * @param defaultExecutionArgs the default execution args
   */
  public void setDefaultExecutionArgs(ExecutionArgs defaultExecutionArgs) {
    this.defaultExecutionArgs = defaultExecutionArgs;
  }
}
