package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.ScriptInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("CUSTOM")
@Data
@Value
@Builder
public class CustomStepInfo implements StepInfo {
  @NotNull private StepType type = StepType.CUSTOM;
  @NotNull public static final StateType stateType = StateType.builder().type(StepType.CUSTOM.name()).build();

  @NotEmpty private String identifier;
  private List<ScriptInfo> scriptInfos = new ArrayList<>();

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
