package software.wings.sm;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.beans.EntityType;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

/**
 * Represents a state object.
 *
 * @author Rishi
 */
public abstract class State {
  @SchemaIgnore private String name;

  @SchemaIgnore private ContextElementType requiredContextElementType;

  @SchemaIgnore private String stateType;

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
   * Gets required context element type.
   *
   * @return the required context element type
   */
  @SchemaIgnore
  public ContextElementType getRequiredContextElementType() {
    return requiredContextElementType;
  }

  /**
   * Sets required context element type.
   *
   * @param requiredContextElementType the required context element type
   */
  @SchemaIgnore
  public void setRequiredContextElementType(ContextElementType requiredContextElementType) {
    this.requiredContextElementType = requiredContextElementType;
  }

  /**
   * Gets state type.
   *
   * @return the state type
   */
  @SchemaIgnore
  public String getStateType() {
    return stateType;
  }

  /**
   * Sets state type.
   *
   * @param stateType the state type
   */
  @SchemaIgnore
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
   * Handle abort event.
   *
   * @param context the context
   */
  public abstract void handleAbortEvent(ExecutionContext context);

  /**
   * Gets required execution argument types.
   *
   * @return the required execution argument types
   */
  @SchemaIgnore
  public List<EntityType> getRequiredExecutionArgumentTypes() {
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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    return executionResponse;
  }

  /**
   * Resolve properties.
   */
  public void resolveProperties() {}
}
