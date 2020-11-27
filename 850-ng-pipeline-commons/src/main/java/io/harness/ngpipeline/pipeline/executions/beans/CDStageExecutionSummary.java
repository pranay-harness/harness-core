package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CDStageExecutionSummaryKeys")
public class CDStageExecutionSummary implements StageExecutionSummary {
  private String planExecutionId;
  private String planNodeId;
  @NotNull private String nodeExecutionId;
  private String stageIdentifier;
  private String stageName;
  private ServiceExecutionSummary serviceExecutionSummary;
  private String serviceDefinitionType;
  private ExecutionStatus executionStatus;
  private Long startedAt;
  private Long endedAt;
  private String serviceIdentifier;
  private String envIdentifier;
  private ExecutionErrorInfo errorInfo;
}
