package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.WorkflowType;

@Data
@Builder
public class ServiceInfraWorkflow {
  private String infraMappingId;
  private String infraMappingName;
  private String workflowId;
  private String workflowName;
  private WorkflowType workflowType;
}
