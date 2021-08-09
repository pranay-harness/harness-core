package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierInstanceWorkflowKeys")
public class BarrierInstanceWorkflow {
  private String uuid;

  private String pipelineStageId;
  private String pipelineStageExecutionId;

  private String workflowExecutionId;

  private String phaseUuid;
  private String phaseExecutionId;

  private String stepUuid;
  private String stepExecutionId;

  public String getUniqueWorkflowKeyInPipeline() {
    return pipelineStageId + uuid;
  }
}
