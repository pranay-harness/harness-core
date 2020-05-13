package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonTypeName("CLEANUP")
@Data
@Value
@Builder
public class CleanupStepInfo implements StepInfo {
  @NotNull private StepType type = StepType.CLEANUP;
  @NotNull public static final StateType stateType = StateType.builder().type(StepType.CLEANUP.name()).build();

  @NotEmpty private String identifier;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public StateType getStateType() {
    return stateType;
  }

  @Override
  public String getStepIdentifier() {
    return identifier;
  }
}
