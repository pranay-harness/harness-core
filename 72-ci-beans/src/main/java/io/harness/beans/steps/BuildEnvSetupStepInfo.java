package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("SETUP_ENV")
@Data
@Value
@Builder
public class BuildEnvSetupStepInfo implements StepInfo {
  @NotNull private StepInfoType type = StepInfoType.SETUP_ENV;
  @NotNull public static final StepType stateType = StepType.builder().type(StepInfoType.SETUP_ENV.name()).build();

  @NotNull private BuildJobEnvInfo buildJobEnvInfo;
  @NotNull private String gitConnectorIdentifier;

  @NotNull private String identifier;

  @Override
  public StepInfoType getType() {
    return type;
  }

  @Override
  public StepType getStateType() {
    return stateType;
  }

  @Override
  public String getStepIdentifier() {
    return identifier;
  }
}
