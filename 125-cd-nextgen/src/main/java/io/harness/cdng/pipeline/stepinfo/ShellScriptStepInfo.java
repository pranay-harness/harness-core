package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.utils.PlanCreatorFacilitatorUtils;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.ShellScriptStepInfoVisitorHelper;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.state.StepType;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.states.ShellScriptState;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecType.SHELL_SCRIPT)
@SimpleVisitorHelper(helperClass = ShellScriptStepInfoVisitorHelper.class)
public class ShellScriptStepInfo extends ShellScriptStepParameters implements CDStepInfo, Visitable {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepInfo(boolean executeOnDelegate, String host, List<String> tags,
      ShellScriptState.ConnectionType connectionType, String sshKeyRef, String connectionAttributes, String commandPath,
      ScriptType scriptType, String scriptString, String timeoutSecs, String outputVars, String sweepingOutputName,
      String sweepingOutputScope, String name, String identifier) {
    super(executeOnDelegate, host, tags, connectionType, sshKeyRef, connectionAttributes, commandPath, scriptType,
        scriptString, timeoutSecs, outputVars, sweepingOutputName, sweepingOutputScope);
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return ShellScriptStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return PlanCreatorFacilitatorUtils.decideTaskFacilitatorType();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return null;
  }
}
