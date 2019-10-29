package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StateExecutionInstance;

@Data
@Builder
public class CVCertifiedDetailsForWorkflowState {
  String workflowName;
  String workflowId;
  String workflowExecutionId;
  String pipelineId;
  String pipelineName;
  String pipelineExecutionId;
  String phaseName;
  String stateExecutionId;
  StateExecutionInstance executionDetails;
}
