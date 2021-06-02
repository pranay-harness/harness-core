package io.harness.cdng.pipeline;

import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.pms.execution.ExecutionStatus;

public class DeploymentStageTypeToStageExecutionSummaryMapper
    implements StageTypeToStageExecutionSummaryMapper<DeploymentStage> {
  @Override
  public StageExecutionSummary getStageExecution(DeploymentStage stageType, String planNodeId, String executionId) {
    return CDStageExecutionSummary.builder()
        .executionStatus(ExecutionStatus.NOTSTARTED)
        .stageIdentifier(stageType.getIdentifier())
        .stageName(stageType.getName())
        .planNodeId(planNodeId)
        .planExecutionId(executionId)
        .build();
  }
}
