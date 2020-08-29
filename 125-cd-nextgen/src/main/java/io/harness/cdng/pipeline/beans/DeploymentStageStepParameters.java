package io.harness.cdng.pipeline.beans;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DeploymentStageStepParameters implements StepParameters {
  private DeploymentStage deploymentStage;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
