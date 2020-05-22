package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.ScriptInfo;
import io.harness.state.StepType;
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
public class TestStepInfo implements StepInfo {
  @NotNull private CIStepType type = CIStepType.TEST;
  @javax.validation.constraints.NotNull
  private StepType stateType = StepType.builder().type(CIStepType.TEST.name()).build();

  @NotEmpty private String identifier;
  @NotEmpty private String numParallel;
  private List<ScriptInfo> scriptInfos;

  @Override
  public CIStepType getType() {
    return type;
  }

  @Override
  public String getStepIdentifier() {
    return identifier;
  }
}