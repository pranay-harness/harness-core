package io.harness.ngpipeline.pipeline;

import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.yaml.core.intfc.StageType;

public interface StageTypeToStageExecutionSummaryMapper<T extends StageType> {
  StageExecutionSummary getStageExecution(T stageType, String planNodeId, String executionId);
}
