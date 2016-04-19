/**
 *
 */
package software.wings.sm;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public abstract class State implements Serializable {
  protected static final long serialVersionUID = 1L;

  private String name;

  private String stateType;

  public State(String name, String stateType) {
    this.name = name;
    this.stateType = stateType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStateType() {
    return stateType;
  }

  public void setStateType(String stateType) {
    this.stateType = stateType;
  }

  public abstract ExecutionResponse execute(ExecutionContext context);

  public Transition handleEvent(ExecutionContext context, StateMachine sm, StateEvent event, Exception ex) {
    return null;
  }

  @Override
  public String toString() {
    return "State [name=" + name + ", stateType=" + stateType + "]";
  }

  public ExecutionResponse handleAsynchResponse(
      ExecutionContext context, Map<String, ? extends Serializable> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    return executionResponse;
  }
}
