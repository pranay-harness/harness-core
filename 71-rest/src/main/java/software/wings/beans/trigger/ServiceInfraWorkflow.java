package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class ServiceInfraWorkflow {
  private String infraMappingId;
  private String infraMappingName;
  private String workflowId;
  private String workflowName;
  private WorkflowType workflowType;
}
