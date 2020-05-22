package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.state.StepType;
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
  @NotNull private StepInfoType type = StepInfoType.CLEANUP;
  @NotNull public static final StepType stateType = StepType.builder().type(StepInfoType.CLEANUP.name()).build();

  @NotEmpty private String identifier;

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
