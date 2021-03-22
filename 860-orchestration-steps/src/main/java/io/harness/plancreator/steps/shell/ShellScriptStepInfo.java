package io.harness.plancreator.steps.shell;

import io.harness.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.shellScript.ExecutionTarget;
import io.harness.steps.shellScript.ShellScriptBaseStepInfo;
import io.harness.steps.shellScript.ShellScriptSourceWrapper;
import io.harness.steps.shellScript.ShellScriptStep;
import io.harness.steps.shellScript.ShellScriptStepParameters;
import io.harness.steps.shellScript.ShellType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

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
public class ShellScriptStepInfo extends ShellScriptBaseStepInfo implements PMSStepInfo, Visitable, WithRollbackInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;
  List<NGVariable> outputVariables;
  List<NGVariable> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepInfo(ShellType shell, ShellScriptSourceWrapper source, ExecutionTarget executionTarget,
      ParameterField<Boolean> onDelegate, String name, String identifier, List<NGVariable> outputVariables,
      List<NGVariable> environmentVariables) {
    super(shell, source, executionTarget, onDelegate);
    this.name = name;
    this.identifier = identifier;
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
  }


  @Override
  @JsonIgnore
  public StepType getStepType() {
    return ShellScriptStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(StepSpecTypeConstants.SHELL_SCRIPT).isPartOfFQN(false).build();
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return ShellScriptStepParameters.infoBuilder()
        .executionTarget(getExecutionTarget())
        .onDelegate(getOnDelegate())
        .outputVariables(NGVariablesUtils.getMapOfVariables(outputVariables, 0L))
        .environmentVariables(NGVariablesUtils.getMapOfVariables(environmentVariables, 0L))
        .rollbackInfo(baseStepParameterInfo.getRollbackInfo())
        .shellType(getShell())
        .source(getSource())
        .timeout(baseStepParameterInfo.getTimeout())
        .name(baseStepParameterInfo.getName())
        .identifier(baseStepParameterInfo.getIdentifier())
        .description(baseStepParameterInfo.getDescription())
        .skipCondition(baseStepParameterInfo.getSkipCondition())
        .build();
  }
}
