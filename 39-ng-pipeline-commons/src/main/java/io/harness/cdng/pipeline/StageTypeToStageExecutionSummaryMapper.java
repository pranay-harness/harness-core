package io.harness.cdng.pipeline;

import io.harness.cdng.pipeline.executions.beans.StageExecutionSummary;
import io.harness.registries.RegistrableEntity;
import io.harness.yaml.core.intfc.StageType;

public interface StageTypeToStageExecutionSummaryMapper<T extends StageType> extends RegistrableEntity {
  StageExecutionSummary getStageExecution(T stageType, String planNodeId, String executionId);

  String getServiceIdentifier(T stageType);

  String getEnvironmentIdentifier(T stageType);

  String getServiceDefinitionType(T stageType);
}
