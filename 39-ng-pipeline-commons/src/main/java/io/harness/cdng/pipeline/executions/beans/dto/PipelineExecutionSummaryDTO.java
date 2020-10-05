package io.harness.cdng.pipeline.executions.beans.dto;

import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.beans.ExecutionErrorInfo;
import io.harness.cdng.pipeline.executions.beans.ExecutionTriggerInfo;
import io.harness.pipeline.executions.NGStageType;
import io.harness.yaml.core.Tag;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PipelineExecutionSummaryDTO {
  String pipelineIdentifier;
  String pipelineName;
  String deploymentId;
  String planExecutionId;
  ExecutionStatus executionStatus;
  Long startedAt;
  Long endedAt;
  List<Tag> tags;
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
}
