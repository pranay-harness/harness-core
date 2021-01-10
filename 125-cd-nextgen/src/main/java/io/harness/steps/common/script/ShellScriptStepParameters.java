package io.harness.steps.common.script;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("shellScriptStepParameters")
public class ShellScriptStepParameters implements StepParameters {
  @NotNull ShellType shell;
  @NotNull ShellScriptSourceWrapper source;
  List<NGVariable> environmentVariables;
  List<NGVariable> outputVariables;
  ExecutionTarget executionTarget;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> onDelegate;
}
