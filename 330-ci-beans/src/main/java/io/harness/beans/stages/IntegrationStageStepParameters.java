package io.harness.beans.stages;

import io.harness.ci.beans.entities.BuildNumber;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class IntegrationStageStepParameters implements StepParameters {
  private IntegrationStage integrationStage;
  private BuildNumber buildNumber;
  private String podName;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
