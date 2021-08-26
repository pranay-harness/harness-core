package io.harness.steps.shellscript;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

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
@RecasterAlias("io.harness.steps.shellscript.ShellScriptStepParameters")
public class ShellScriptStepParameters extends ShellScriptBaseStepInfo implements SpecParameters {
  Map<String, Object> outputVariables;
  Map<String, Object> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepParameters(ShellType shellType, ShellScriptSourceWrapper source,
      ExecutionTarget executionTarget, ParameterField<Boolean> onDelegate, Map<String, Object> outputVariables,
      Map<String, Object> environmentVariables, ParameterField<List<String>> delegateSelectors) {
    super(shellType, source, executionTarget, onDelegate, delegateSelectors);
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
  }
}
