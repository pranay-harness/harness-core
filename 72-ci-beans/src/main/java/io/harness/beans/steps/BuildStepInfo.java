package io.harness.beans.steps;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.script.ScriptInfo;
import io.harness.state.StepType;
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
public class BuildStepInfo implements StepInfo {
  @NotNull private CIStepType type = CIStepType.BUILD;
  @NotNull public static final StepType stateType = StepType.builder().type(CIStepType.BUILD.name()).build();
  private List<ScriptInfo> scriptInfos;
  @NotEmpty private String identifier;

  @Override
  public CIStepType getType() {
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
