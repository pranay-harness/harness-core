package io.harness.steps.shellScript;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("ShellScriptBaseStepInfo")
public class ShellScriptBaseStepInfo {
  @NotNull ShellType shell;
  @NotNull ShellScriptSourceWrapper source;
  ExecutionTarget executionTarget;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> onDelegate;
}
