package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionGraph;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class PipelineExecutionDetail {
  PipelineExecutionSummaryDTO pipelineExecution;
  ExecutionGraph stageGraph;
  ExecutionGraph stageRollbackGraph;
}
