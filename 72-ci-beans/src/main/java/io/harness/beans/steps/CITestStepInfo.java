package io.harness.beans.steps;

import io.harness.beans.script.CIScriptInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@Value
@Builder
public class CITestStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.TEST;
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