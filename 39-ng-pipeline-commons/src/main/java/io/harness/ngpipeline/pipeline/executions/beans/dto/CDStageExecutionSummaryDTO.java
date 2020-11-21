package io.harness.ngpipeline.pipeline.executions.beans.dto;

import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.ExecutionErrorInfo;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("stage")
public class CDStageExecutionSummaryDTO implements StageExecutionSummaryDTO {
  String planExecutionId;
  String stageIdentifier;
  ServiceExecutionSummary serviceInfo;
  String stageName;
  String serviceDefinitionType;
  ExecutionStatus executionStatus;
  Long startedAt;
  Long endedAt;
  String serviceIdentifier;
  String envIdentifier;
  ExecutionErrorInfo errorInfo;
}
