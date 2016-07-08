/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import software.wings.sm.ExecutionEventType;

/**
 * @author Rishi
 *
 */
@Entity(value = "workflowExecutionEvent", noClassnameStored = true)
public class WorkflowExecutionEvent extends Base {
  private ExecutionEventType executionEventType;

  private String envId;
  private String workflowExecutionId;
  private String stateExecutionInstanceId;

  public ExecutionEventType getExecutionEventType() {
    return executionEventType;
  }

  public void setExecutionEventType(ExecutionEventType executionEventType) {
    this.executionEventType = executionEventType;
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
