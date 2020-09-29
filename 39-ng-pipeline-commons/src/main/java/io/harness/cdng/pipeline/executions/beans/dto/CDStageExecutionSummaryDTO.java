package io.harness.cdng.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("stage")
public class CDStageExecutionSummaryDTO implements StageExecutionSummaryDTO {
  String planExecutionId;
  String stageIdentifier;
  String stageName;
  String serviceDefinitionType;
  ExecutionStatus executionStatus;
  Long startedAt;
  Long endedAt;
  String serviceIdentifier;
  String envIdentifier;
  String errorMsg;
}
