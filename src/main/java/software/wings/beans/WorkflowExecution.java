/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.sm.ExecutionStatus;

/**
 * @author Rishi
 */
@Entity(value = "workflowExecutions", noClassnameStored = true)
public class WorkflowExecution extends Base {
  @Indexed private String workflowId;

  private String stateMachineId;
  @Indexed private WorkflowExecutionType workflowExecutionType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;
  @Transient private Graph graph;

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public WorkflowExecutionType getWorkflowExecutionType() {
    return workflowExecutionType;
  }

  public void setWorkflowExecutionType(WorkflowExecutionType workflowExecutionType) {
    this.workflowExecutionType = workflowExecutionType;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public enum WorkflowExecutionType { PIPELINE, ORCHESTRATION, SIMPLE, SUB_WORKFLOW }
}
