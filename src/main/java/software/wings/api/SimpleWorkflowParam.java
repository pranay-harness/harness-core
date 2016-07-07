/**
 *
 */

package software.wings.api;

import software.wings.beans.ExecutionStrategy;
import software.wings.common.Constants;

import java.util.HashMap;
import java.util.List;
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

  public static final class Builder {
    private ExecutionStrategy executionStrategy;
    private String serviceId;
    private String commandName;
    private List<String> instanceIds;

    private Builder() {}

    public static Builder aSimpleWorkflowParam() {
      return new Builder();
    }

    public Builder withExecutionStrategy(ExecutionStrategy executionStrategy) {
      this.executionStrategy = executionStrategy;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public Builder but() {
      return aSimpleWorkflowParam()
          .withExecutionStrategy(executionStrategy)
          .withServiceId(serviceId)
          .withCommandName(commandName)
          .withInstanceIds(instanceIds);
    }

    public SimpleWorkflowParam build() {
      SimpleWorkflowParam simpleWorkflowParam = new SimpleWorkflowParam();
      simpleWorkflowParam.setExecutionStrategy(executionStrategy);
      simpleWorkflowParam.setServiceId(serviceId);
      simpleWorkflowParam.setCommandName(commandName);
      simpleWorkflowParam.setInstanceIds(instanceIds);
      return simpleWorkflowParam;
    }
  }
}
