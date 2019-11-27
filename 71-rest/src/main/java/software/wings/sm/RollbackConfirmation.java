package software.wings.sm;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;

import java.util.List;

@Value
@Builder
public class RollbackConfirmation {
  private String validationMessage;
  private WorkflowExecution activeWorkflowExecution;
  private String workflowId;
  private boolean valid;
  private List<Artifact> artifacts;
}
