package io.harness.cdng.pipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.execution.beans.ExecutionGraph;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PipelineExecutionDetail {
  PipelineExecutionSummaryDTO pipelineExecution;
  ExecutionGraph stageGraph;
  ExecutionGraph stageRollbackGraph;
}
