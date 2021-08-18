package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class RollbackConfirmation {
  private String validationMessage;
  private WorkflowExecution activeWorkflowExecution;
  private String workflowId;
  private boolean valid;
  private List<Artifact> artifacts;
}
