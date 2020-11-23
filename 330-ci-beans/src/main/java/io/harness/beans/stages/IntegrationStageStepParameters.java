package io.harness.beans.stages;

import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.state.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntegrationStageStepParameters implements StepParameters {
  private IntegrationStage integrationStage;
  private BuildNumberDetails buildNumberDetails;
  private BuildStatusUpdateParameter buildStatusUpdateParameter;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
