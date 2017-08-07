package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public class WorkflowVariable {
  String workflowId;
  String workflowName;
  List<Variable> variables = new ArrayList<>();

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public void setVariables(List<Variable> variables) {
    this.variables = variables;
  }

  public static final class Builder {
    String workflowId;
    String workflowName;
    List<Variable> variables = new ArrayList<>();

    private Builder() {}

    public static Builder aWorkflowVariable() {
      return new Builder();
    }

    public Builder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }
    public Builder withWorkflowName(String workflowName) {
      this.workflowName = workflowName;
      return this;
    }
    public Builder withVariables(List<Variable> variables) {
      this.variables = variables;
      return this;
    }

    public WorkflowVariable build() {
      WorkflowVariable workflowVariable = new WorkflowVariable();
      workflowVariable.setWorkflowId(this.workflowId);
      workflowVariable.setWorkflowName(this.workflowName);
      workflowVariable.setVariables(variables);
      return workflowVariable;
    }
  }
}
