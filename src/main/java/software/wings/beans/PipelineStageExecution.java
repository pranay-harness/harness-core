package software.wings.beans;

import java.util.List;

/**
 * Created by anubhaw on 10/26/16.
 */
public class PipelineStageExecution {
  private List<WorkflowExecution> workflowExecutions;

  public PipelineStageExecution(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }

  public List<WorkflowExecution> getWorkflowExecutions() {
    return workflowExecutions;
  }

  public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }
}
