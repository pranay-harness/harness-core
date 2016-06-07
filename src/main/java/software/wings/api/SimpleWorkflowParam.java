/**
 *
 */
package software.wings.api;

import software.wings.beans.ExecutionStrategy;
import software.wings.common.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class SimpleWorkflowParam extends ServiceInstanceIdsParam {
  private ExecutionStrategy executionStrategy;

  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = super.paramMap();
    if (map == null) {
      map = new HashMap<>();
    }
    map.put(Constants.SIMPLE_WORKFLOW_REPEAT_STRATEGY, executionStrategy);
    return map;
  }
}
