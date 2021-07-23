package io.harness.cdng.pipeline.stepinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.ShellScriptStepInfoVisitorHelper;
import io.harness.data.structure.CollectionUtils;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.common.script.ExecutionTarget;
import io.harness.steps.common.script.ShellScriptBaseStepInfo;
import io.harness.steps.common.script.ShellScriptSourceWrapper;
import io.harness.steps.common.script.ShellScriptStep;
import io.harness.steps.common.script.ShellScriptStepParameters;
import io.harness.steps.common.script.ShellType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.inject.Inject;
import io.swagger.annotations.ApiModelProperty;
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
@OwnedBy(HarnessTeam.CDC)
public class ShellScriptStepInfo extends ShellScriptBaseStepInfo implements CDStepInfo, Visitable {
  @Inject private static ShellScriptBaseStepInfo shellScriptBaseStepInfo;
  List<NGVariable> outputVariables;
  List<NGVariable> environmentVariables;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepInfo(ShellType shell, ShellScriptSourceWrapper source, ExecutionTarget executionTarget,
      ParameterField<Boolean> onDelegate, List<NGVariable> outputVariables, List<NGVariable> environmentVariables,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(shell, source, executionTarget, onDelegate);
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
    this.delegateSelectors = delegateSelectors;
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
  public SpecParameters getSpecParameters() {
    ExecutionTarget executionTarget = getExecutionTarget();
    executionTarget = getTrimmedExecutionTarget(executionTarget);
    return ShellScriptStepParameters.infoBuilder()
        .executionTarget(executionTarget)
        .onDelegate(getOnDelegate())
        .outputVariables(NGVariablesUtils.getMapOfVariables(outputVariables, 0L))
        .environmentVariables(NGVariablesUtils.getMapOfVariables(environmentVariables, 0L))
        .shellType(getShell())
        .source(getSource())
        .delegateSelectors(ParameterField.createValueField(
            CollectionUtils.emptyIfNull(delegateSelectors != null ? delegateSelectors.getValue() : null)))
        .build();
  }

  private ExecutionTarget getTrimmedExecutionTarget(ExecutionTarget executionTarget) {
    String host = executionTarget.getHost().getValue();
    String connecRef = executionTarget.getConnectorRef().getValue();
    String workingDir = executionTarget.getWorkingDirectory().getValue();
    return ExecutionTarget.builder()
        .host(ParameterField.createValueField(host == null ? host : host.trim()))
        .connectorRef(ParameterField.createValueField(connecRef == null ? connecRef : connecRef.trim()))
        .workingDirectory(ParameterField.createValueField(workingDir == null ? workingDir : workingDir.trim()))
        .build();
  }
}
