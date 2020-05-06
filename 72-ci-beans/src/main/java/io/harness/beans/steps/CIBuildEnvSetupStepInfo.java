package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.environment.CIBuildJobEnvInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("SETUP_ENV")
@Data
@Value
@Builder
public class CIBuildEnvSetupStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.SETUP_ENV;
  @NotNull public static final StateType stateType = StateType.builder().type(StepType.SETUP_ENV.name()).build();

  @NotNull private CIBuildJobEnvInfo ciBuildJobEnvInfo;
  @NotNull private String name;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public StateType getStateType() {
    return stateType;
  }

  @Override
  public String getStepName() {
    return name;
  }
}
