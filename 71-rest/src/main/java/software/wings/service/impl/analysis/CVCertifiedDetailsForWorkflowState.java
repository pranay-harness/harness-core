package software.wings.service.impl.analysis;

import software.wings.sm.StateExecutionInstance;

import lombok.Builder;
import lombok.Data;

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
