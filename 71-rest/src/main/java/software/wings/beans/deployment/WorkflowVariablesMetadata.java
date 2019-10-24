package software.wings.beans.deployment;

import lombok.Value;
import software.wings.beans.Variable;

import java.util.List;

@Value
public class WorkflowVariablesMetadata {
  private List<Variable> workflowVariables;
  private boolean changed;
  private String changedMessage;

  public WorkflowVariablesMetadata(List<Variable> workflowVariables, boolean changed, String changedMessage) {
    this.workflowVariables = workflowVariables;
    this.changed = changed;
    this.changedMessage = changedMessage;
  }

  public WorkflowVariablesMetadata(List<Variable> workflowVariables) {
    this(workflowVariables, false, null);
  }
}
