package io.harness.cdng.pipeline.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("deploymentStageStepParameters")
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.pipeline.beans.DeploymentStageStepParameters")
public class DeploymentStageStepParameters implements SpecParameters {
  String childNodeID;

  public static DeploymentStageStepParameters getStepParameters(String childNodeID) {
    return DeploymentStageStepParameters.builder().childNodeID(childNodeID).build();
  }

  @Override
  public SpecParameters getViewJsonObject() {
    return DeploymentStageStepParameters.builder().build();
  }
}
