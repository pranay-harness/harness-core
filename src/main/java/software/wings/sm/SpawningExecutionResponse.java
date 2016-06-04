package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by rishi on 5/20/16.
 */
public class SpawningExecutionResponse extends ExecutionResponse {
  private List<StateExecutionInstance> stateExecutionInstanceList = new ArrayList<>();

  public List<StateExecutionInstance> getStateExecutionInstanceList() {
    return stateExecutionInstanceList;
  }

  public void setStateExecutionInstanceList(List<StateExecutionInstance> stateExecutionInstanceList) {
    this.stateExecutionInstanceList = stateExecutionInstanceList;
  }

  /**
   * Adds the.
   *
   * @param stateExecutionInstance the state execution instance
   */
  public void add(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstanceList.add(stateExecutionInstance);
  }
}
