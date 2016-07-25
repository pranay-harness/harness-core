/**
 *
 */
package software.wings.beans;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Rishi
 *
 */
public class RequiredExecutionArgs {
  private Set<ExecutionArgumentType> requiredExecutionTypes = new HashSet<>();
  private ExecutionArgs defaultExecutionArgs = new ExecutionArgs();

  public Set<ExecutionArgumentType> getRequiredExecutionTypes() {
    return requiredExecutionTypes;
  }

  public void setRequiredExecutionTypes(Set<ExecutionArgumentType> requiredExecutionTypes) {
    this.requiredExecutionTypes = requiredExecutionTypes;
  }

  public ExecutionArgs getDefaultExecutionArgs() {
    return defaultExecutionArgs;
  }

  public void setDefaultExecutionArgs(ExecutionArgs defaultExecutionArgs) {
    this.defaultExecutionArgs = defaultExecutionArgs;
  }
}
