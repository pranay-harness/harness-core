package io.harness.steps.common.script;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepSpecParameters;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
public class ShellScriptStepParameters extends ShellScriptBaseStepInfo implements StepSpecParameters {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  Map<String, Object> outputVariables;
  Map<String, Object> environmentVariables;
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepParameters(ShellType shellType, ShellScriptSourceWrapper source,
      ExecutionTarget executionTarget, ParameterField<Boolean> onDelegate, ParameterField<String> timeout,
      Map<String, Object> outputVariables, Map<String, Object> environmentVariables,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(shellType, source, executionTarget, onDelegate);
    this.timeout = timeout;
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
    this.delegateSelectors = delegateSelectors;
  }
}
