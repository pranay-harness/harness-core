package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

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

  public void add(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstanceList.add(stateExecutionInstance);
  }
}
