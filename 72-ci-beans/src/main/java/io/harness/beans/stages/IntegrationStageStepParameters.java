package io.harness.beans.stages;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class IntegrationStageStepParameters implements StepParameters {
  private IntegrationStage integrationStage;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
