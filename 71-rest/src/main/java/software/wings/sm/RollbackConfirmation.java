package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class RollbackConfirmation {
  private String validationMessage;
  private WorkflowExecution activeWorkflowExecution;
  private String workflowId;
  private boolean valid;
  private List<Artifact> artifacts;
}
