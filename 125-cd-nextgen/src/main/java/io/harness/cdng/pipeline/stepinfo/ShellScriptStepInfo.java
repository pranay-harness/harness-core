package io.harness.cdng.pipeline.stepinfo;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.ShellScriptStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.common.script.ExecutionTarget;
import io.harness.steps.common.script.ShellScriptBaseStepInfo;
import io.harness.steps.common.script.ShellScriptSourceWrapper;
import io.harness.steps.common.script.ShellScriptStep;
import io.harness.steps.common.script.ShellScriptStepParameters;
import io.harness.steps.common.script.ShellType;
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
public class ShellScriptStepInfo extends ShellScriptBaseStepInfo implements CDStepInfo, Visitable {
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
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SHELL_SCRIPT_STEP).build();
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
