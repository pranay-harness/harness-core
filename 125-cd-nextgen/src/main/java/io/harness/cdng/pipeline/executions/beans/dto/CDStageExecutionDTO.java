package io.harness.cdng.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.executions.beans.PipelineExecutionStatus;
import io.harness.yaml.core.Artifact;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonTypeName("CDStage")
public class CDStageExecutionDTO implements StageExecutionDTO {
  String planExecutionId;
  String stageIdentifier;
  String stageName;
  List<Artifact> artifactsDeployed;
  ServiceDefinitionType deploymentType;
  PipelineExecutionStatus pipelineExecutionStatus;
  Long startedAt;
  long endedAt;
  String serviceIdentifier;
  String envIdentifier;
  String errorMsg;
}
