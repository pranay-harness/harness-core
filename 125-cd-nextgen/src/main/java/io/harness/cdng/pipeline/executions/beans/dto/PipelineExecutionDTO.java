package io.harness.cdng.pipeline.executions.beans.dto;

import io.harness.cdng.pipeline.executions.PipelineExecutionStatus;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.governance.pipeline.service.model.Tag;
import io.harness.ng.core.user.User;
import io.harness.yaml.core.intfc.StageType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PipelineExecutionDTO {
  String pipelineIdentifier;
  String pipelineName;
  String planExecutionId;
  PipelineExecutionStatus pipelineExecutionStatus;
  long startedAt;
  long endedAt;
  User triggeredBy;
  TriggerType triggerType;
  List<Tag> tags;
  List<StageExecutionDTO> stageExecutionSummaryElements;
  String errorMsg;
  List<String> stageIdentifiers;
  List<String> serviceIdentifiers;
  List<String> envIdentifiers;
  List<ServiceDefinitionType> serviceDefinitionTypes;
  List<StageType> stageTypes;
  int successfulStagesCount;
  int runningStagesCount;
  int failedStagesCount;
}
