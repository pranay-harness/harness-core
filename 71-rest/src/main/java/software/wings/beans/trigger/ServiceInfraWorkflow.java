package software.wings.beans.trigger;

import io.harness.beans.WorkflowType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceInfraWorkflow {
  private String infraMappingId;
  private String infraMappingName;
  private String workflowId;
  private String workflowName;
  private WorkflowType workflowType;
}
