package software.wings.infra;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;

import java.util.List;

@Value
@Builder
public class InfraMappingDetail {
  private InfrastructureMapping infrastructureMapping;
  private List<WorkflowExecution> workflowExecutionList;
}
