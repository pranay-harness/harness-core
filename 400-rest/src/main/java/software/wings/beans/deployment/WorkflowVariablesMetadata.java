package software.wings.beans.deployment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Variable;

import java.util.List;
import lombok.Value;

@OwnedBy(CDC)
@Value
@TargetModule(HarnessModule._957_CG_BEANS)
public class WorkflowVariablesMetadata {
  private static final String CHANGED_MESSAGE =
      "Workflow Variables have changed since previous execution. Please select new values.";

  private List<Variable> workflowVariables;
  private boolean changed;
  private String changedMessage;

  public WorkflowVariablesMetadata(List<Variable> workflowVariables, boolean changed) {
    this.workflowVariables = workflowVariables;
    this.changed = changed;
    if (changed) {
      this.changedMessage = CHANGED_MESSAGE;
    } else {
      this.changedMessage = null;
    }
  }

  public WorkflowVariablesMetadata(List<Variable> workflowVariables) {
    this(workflowVariables, false);
  }
}
