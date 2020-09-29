package io.harness.cdng.pipeline;

import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.StageExecutionSummary;

public class DeploymentStageTypeToStageExecutionSummaryMapper
    implements StageTypeToStageExecutionSummaryMapper<DeploymentStage> {
  @Override
  public StageExecutionSummary getStageExecution(DeploymentStage stageType, String planNodeId, String executionId) {
    return CDStageExecutionSummary.builder()
        .serviceDefinitionType(stageType.getService().getServiceDefinition().getType())
        .envIdentifier(getEnvironmentIdentifier(stageType))
        .serviceIdentifier(getServiceIdentifier(stageType))
        .executionStatus(ExecutionStatus.NOT_STARTED)
        .stageIdentifier(stageType.getIdentifier())
        .stageName(stageType.getName())
        .planNodeId(planNodeId)
        .planExecutionId(executionId)
        .build();
  }

  @Override
  public String getServiceIdentifier(DeploymentStage stageType) {
    return stageType.getService().getIdentifier().getValue();
  }

  @Override
  public String getEnvironmentIdentifier(DeploymentStage stageType) {
    return stageType.getInfrastructure().getEnvironment().getIdentifier().getValue();
  }

  @Override
  public String getServiceDefinitionType(DeploymentStage stageType) {
    return stageType.getService().getServiceDefinition().getType();
  }
}
