package io.harness.cdng.pipeline.executions.beans;

import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.executions.beans.PipelineExecutionStatus;
import io.harness.yaml.core.Artifact;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CDStageExecution implements StageExecution {
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
