package io.harness.cdng.pipeline.executions.beans;

import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.yaml.core.Artifact;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CDStageExecutionSummary implements StageExecutionSummary {
  private String planExecutionId;
  private String planNodeId;
  private String stageIdentifier;
  private String stageName;
  private List<Artifact> artifactsDeployed;
  private String serviceDefinitionType;
  private ExecutionStatus executionStatus;
  private Long startedAt;
  private Long endedAt;
  private String serviceIdentifier;
  private String envIdentifier;
  private String errorMsg;
}
