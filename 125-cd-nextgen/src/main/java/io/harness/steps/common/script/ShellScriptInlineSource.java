package io.harness.steps.common.script;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonTypeName("Inline")
public class ShellScriptInlineSource implements ShellScriptBaseSource {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> script;

  @Override
  public String getType() {
    return "Inline";
  }
}
