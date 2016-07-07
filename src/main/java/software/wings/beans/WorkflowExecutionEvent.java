/**
 *
 */
package software.wings.beans;

import software.wings.sm.StateEvent;

/**
 * @author Rishi
 *
 */
public class WorkflowExecutionEvent extends Base {
  private StateEvent stateEvent;

  private String envId;
  private String workflowExecutionId;
  private String stateExecutionInstanceId;

  public StateEvent getStateEvent() {
    return stateEvent;
  }

  public void setStateEvent(StateEvent stateEvent) {
    this.stateEvent = stateEvent;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }
}
