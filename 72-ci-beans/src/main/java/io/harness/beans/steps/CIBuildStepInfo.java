package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.CIScriptInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("BUILD")
@Data
@Value
@Builder
public class CIBuildStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.BUILD;
  @NotNull public static final StateType stateType = StateType.builder().type(StepType.BUILD.name()).build();

  private List<CIScriptInfo> scriptInfos;
  @NotEmpty private String name;

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
