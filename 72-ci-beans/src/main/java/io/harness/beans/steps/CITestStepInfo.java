package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.CIScriptInfo;
import io.harness.state.StateType;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JsonTypeName("TEST")
@Data
@Value
@Builder
public class CITestStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.TEST;
  @javax.validation.constraints.NotNull
  private StateType stateType = StateType.builder().type(StepType.TEST.name()).build();

  @NotEmpty private String name;
  @NotEmpty private String numParallel;
  private List<CIScriptInfo> scriptInfos;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public String getStepName() {
    return name;
  }
}