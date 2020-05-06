package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.CIScriptInfo;
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
public class CICustomStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.CUSTOM;
  @NotNull private StateType stateType = StateType.builder().type(StepType.CUSTOM.name()).build();

  @NotEmpty private String name;
  private List<CIScriptInfo> scriptInfos = new ArrayList<>();

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public String getStepName() {
    return name;
  }
}
