package io.harness.steps.shellscript;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("shellScriptStepParameters")
@OwnedBy(HarnessTeam.CDC)
public class ShellScriptStepParameters extends ShellScriptBaseStepInfo implements SpecParameters {
  Map<String, Object> outputVariables;
  Map<String, Object> environmentVariables;
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepParameters(ShellType shellType, ShellScriptSourceWrapper source,
      ExecutionTarget executionTarget, ParameterField<Boolean> onDelegate, Map<String, Object> outputVariables,
      Map<String, Object> environmentVariables, ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(shellType, source, executionTarget, onDelegate);
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
    this.delegateSelectors = delegateSelectors;
  }
}
