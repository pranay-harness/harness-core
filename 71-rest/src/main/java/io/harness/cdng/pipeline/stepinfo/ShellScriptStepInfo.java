package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.facilitator.FacilitatorType;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("shellScript")
public class ShellScriptStepInfo implements CDStepInfo, GenericStepInfo {
  String displayName;
  String identifier;
  ShellScriptStepParameters shellScript;

  @Override
  @JsonIgnore
  public StepParameters getStepParameters() {
    return shellScript;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return ShellScriptStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return FacilitatorType.TASK;
  }
}
