package io.harness.beans.steps;

import io.harness.beans.environment.CIBuildJobEnvInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Data
@Value
@Builder
public class CIBuildEnvSetupStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.SETUP_ENV;

  @NotNull private CIBuildJobEnvInfo ciBuildJobEnvInfo;
  @NotNull private String name;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public String getStepName() {
    return null;
  }
}
