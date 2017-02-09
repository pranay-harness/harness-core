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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    WorkflowExecutionFilter that = (WorkflowExecutionFilter) o;

    if (workflowIds != null ? !workflowIds.equals(that.workflowIds) : that.workflowIds != null)
      return false;
    return envIds != null ? envIds.equals(that.envIds) : that.envIds == null;
  }

  @Override
  public int hashCode() {
    int result = workflowIds != null ? workflowIds.hashCode() : 0;
    result = 31 * result + (envIds != null ? envIds.hashCode() : 0);
    return result;
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
