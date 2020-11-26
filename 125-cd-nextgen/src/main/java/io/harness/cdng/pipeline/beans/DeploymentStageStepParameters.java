package io.harness.cdng.pipeline.beans;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.state.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("deploymentStageStepParameters")
public class DeploymentStageStepParameters implements StepParameters {
  private DeploymentStage deploymentStage;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
