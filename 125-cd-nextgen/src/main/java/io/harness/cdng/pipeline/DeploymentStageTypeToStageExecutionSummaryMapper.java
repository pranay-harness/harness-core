package io.harness.cdng.pipeline;

import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;

public class DeploymentStageTypeToStageExecutionSummaryMapper
    implements StageTypeToStageExecutionSummaryMapper<DeploymentStage> {
  @Override
  public StageExecutionSummary getStageExecution(DeploymentStage stageType, String planNodeId, String executionId) {
    return CDStageExecutionSummary.builder()
        .executionStatus(ExecutionStatus.NOT_STARTED)
        .stageIdentifier(stageType.getIdentifier())
        .stageName(stageType.getName())
        .planNodeId(planNodeId)
        .planExecutionId(executionId)
        .build();
  }
}
