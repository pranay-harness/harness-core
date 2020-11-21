package io.harness.ngpipeline.pipeline.executions.beans.dto;

import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.ExecutionErrorInfo;
import io.harness.ngpipeline.pipeline.executions.beans.ExecutionTriggerInfo;
import io.harness.pipeline.executions.NGStageType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineExecutionSummaryDTO {
  String pipelineIdentifier;
  String pipelineName;
  String deploymentId;
  String planExecutionId;
  ExecutionStatus executionStatus;
  String inputSetYaml;
  Long startedAt;
  Long endedAt;
  Map<String, String> tags;
  List<StageExecutionSummaryDTO> stageExecutionSummaryElements;
  String errorMsg;
  List<String> stageIdentifiers;
  List<String> serviceIdentifiers;
  List<String> envIdentifiers;
  List<String> serviceDefinitionTypes;
  List<NGStageType> stageTypes;
  ExecutionErrorInfo errorInfo;
  ExecutionTriggerInfo triggerInfo;
  long successfulStagesCount;
  long runningStagesCount;
  long failedStagesCount;
  long totalStagesCount;
}
