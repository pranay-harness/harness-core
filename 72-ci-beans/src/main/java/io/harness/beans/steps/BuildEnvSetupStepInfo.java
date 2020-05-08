package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("SETUP_ENV")
@Data
@Value
@Builder
public class BuildEnvSetupStepInfo implements StepInfo {
  @NotNull private StepType type = StepType.SETUP_ENV;
  @NotNull public static final StateType stateType = StateType.builder().type(StepType.SETUP_ENV.name()).build();

  @NotNull private BuildJobEnvInfo buildJobEnvInfo;
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
