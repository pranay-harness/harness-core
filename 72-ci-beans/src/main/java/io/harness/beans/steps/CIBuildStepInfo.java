package io.harness.beans.steps;

import io.harness.beans.script.CIScriptInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Value
@Builder
public class CIBuildStepInfo implements CIStepInfo {
  @NotNull private StepType type = StepType.BUILD;
  private List<CIScriptInfo> scriptInfos = new ArrayList<>();
  @NotEmpty private String name;

  @Override
  public StepType getType() {
    return type;
  }

  @Override
  public String getStepName() {
    return name;
  }
}
