/**
 *
 */

package software.wings.api;

import software.wings.beans.ExecutionStrategy;
import software.wings.common.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Simple workflow param.
 *
 * @author Rishi
 */
public class SimpleWorkflowParam extends ServiceInstanceIdsParam {
  private ExecutionStrategy executionStrategy;
  private String commandName;

  /**
   * Gets execution strategy.
   *
   * @return the execution strategy
   */
  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  /**
   * Sets execution strategy.
   *
   * @param executionStrategy the execution strategy
   */
  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = super.paramMap();
    if (map == null) {
      map = new HashMap<>();
    }
    map.put(Constants.SIMPLE_WORKFLOW_COMMAND_NAME, commandName);
    map.put(Constants.SIMPLE_WORKFLOW_REPEAT_STRATEGY, executionStrategy);
    return map;
  }
}
