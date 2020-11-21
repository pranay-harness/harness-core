package software.wings.infra;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraMappingDetail {
  private InfrastructureMapping infrastructureMapping;
  private List<WorkflowExecution> workflowExecutionList;
}
