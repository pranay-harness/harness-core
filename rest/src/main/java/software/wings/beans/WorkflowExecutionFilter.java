package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 10/31/16.
 */
public class WorkflowExecutionFilter {
  private List<String> workflowIds = new ArrayList<>();
  private List<String> envIds = new ArrayList<>();

  public List<String> getWorkflowIds() {
    return workflowIds;
  }

  public void setWorkflowIds(List<String> workflowIds) {
    this.workflowIds = workflowIds;
  }

  public List<String> getEnvIds() {
    return envIds;
  }

  public void setEnvIds(List<String> envIds) {
    this.envIds = envIds;
  }

  public static final class WorkflowExecutionFilterBuilder {
    private List<String> workflowIds = new ArrayList<>();
    private List<String> envIds = new ArrayList<>();

    private WorkflowExecutionFilterBuilder() {}

    public static WorkflowExecutionFilterBuilder aWorkflowExecutionFilter() {
      return new WorkflowExecutionFilterBuilder();
    }

    public WorkflowExecutionFilterBuilder addWorkflowId(String workflowId) {
      this.workflowIds.add(workflowId);
      return this;
    }

    public WorkflowExecutionFilterBuilder addEnvId(String envId) {
      this.envIds.add(envId);
      return this;
    }

    public WorkflowExecutionFilter build() {
      WorkflowExecutionFilter workflowExecutionFilter = new WorkflowExecutionFilter();
      workflowExecutionFilter.setWorkflowIds(workflowIds);
      workflowExecutionFilter.setEnvIds(envIds);
      return workflowExecutionFilter;
    }
  }
}
