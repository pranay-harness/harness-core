package io.harness.cdng.pipeline.executions.beans;

import io.harness.cdng.pipeline.executions.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
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
