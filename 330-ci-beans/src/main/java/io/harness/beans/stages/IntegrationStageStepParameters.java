package io.harness.beans.stages;

import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("integrationStageStepParameters")
public class IntegrationStageStepParameters implements StepParameters {
  private IntegrationStage integrationStage;
  private BuildNumberDetails buildNumberDetails;
  private BuildStatusUpdateParameter buildStatusUpdateParameter;
  private Map<String, String> fieldToExecutionNodeIdMap;

  @Override
  public String toJson() {
    return JsonOrchestrationUtils.asJsonWithIgnoredFields(this);
  }
}
