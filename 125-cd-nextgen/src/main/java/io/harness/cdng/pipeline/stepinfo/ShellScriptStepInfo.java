package io.harness.cdng.pipeline.stepinfo;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.ShellScriptStepInfoVisitorHelper;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import software.wings.sm.states.ShellScriptState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.SHELL_SCRIPT)
@SimpleVisitorHelper(helperClass = ShellScriptStepInfoVisitorHelper.class)
@TypeAlias("shellScriptStepInfo")
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
    return OrchestrationFacilitatorType.TASK_V3;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SHELL_SCRIPT_STEP).build();
  }
}
