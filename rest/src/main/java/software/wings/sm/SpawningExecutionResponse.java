package software.wings.sm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 5/20/16.
 */
@SuppressFBWarnings("UUF_UNUSED_FIELD")
public class SpawningExecutionResponse extends ExecutionResponse {
  private List<StateExecutionInstance> stateExecutionInstanceList = new ArrayList<>();
  private String delegateTaskId;

  /**
   * Gets state execution instance list.
   *
   * @return the state execution instance list
   */
  public List<StateExecutionInstance> getStateExecutionInstanceList() {
    return stateExecutionInstanceList;
  }

  /**
   * Sets state execution instance list.
   *
   * @param stateExecutionInstanceList the state execution instance list
   */
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
