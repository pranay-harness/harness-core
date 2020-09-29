package io.harness.cdng.pipeline.executions.beans.dto;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.TriggerType;
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
  String planExecutionId;
  ExecutionStatus executionStatus;
  Long startedAt;
  Long endedAt;
  EmbeddedUser triggeredBy;
  TriggerType triggerType;
  List<Tag> tags;
  List<StageExecutionSummaryDTO> stageExecutionSummaryElements;
  String errorMsg;
  List<String> stageIdentifiers;
  List<String> serviceIdentifiers;
  List<String> envIdentifiers;
  List<String> serviceDefinitionTypes;
  List<NGStageType> stageTypes;
  long successfulStagesCount;
  long runningStagesCount;
  long failedStagesCount;
}
