package software.wings.sm;

import com.github.reinert.jjschema.SchemaIgnore;

import java.io.Serializable;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Represents a state object.
 *
 * @author Rishi
 */
public abstract class State implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  protected static final long serialVersionUID = 1L;

  @SchemaIgnore private String name;

  private String stateType;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public State(String name, String stateType) {
    this.name = name;
    this.stateType = stateType;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  @SchemaIgnore
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  @SchemaIgnore
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets state type.
   *
   * @return the state type
   */
  public String getStateType() {
    return stateType;
  }

  /**
   * Sets state type.
   *
   * @param stateType the state type
   */
  public void setStateType(String stateType) {
    this.stateType = stateType;
  }

  /**
   * Execute.
   *
   * @param context the context
   * @return the execution response
   */
  public abstract ExecutionResponse execute(ExecutionContext context);

  /**
   * Handle event.
   *
   * @param context the context
   * @param sm      the sm
   * @param event   the event
   * @param ex      the ex
   * @return the transition
   */
  public Transition handleEvent(ExecutionContextImpl context, StateMachine sm, StateEvent event, Exception ex) {
    return null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "State [name=" + name + ", stateType=" + stateType + "]";
  }

  /**
   * Callback for handing responses from states that this state was waiting on.
   *
   * @param context  Context of execution.
   * @param response map of responses this state was waiting on.
   * @return Response from handling this state.
   */
  public ExecutionResponse handleAsynchResponse(
      ExecutionContextImpl context, Map<String, ? extends Serializable> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    return executionResponse;
  }
}
