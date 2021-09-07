package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class InlineTerraformVarFileSpec implements TerraformVarFileSpec {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> content;

  @Override
  public String getType() {
    return TerraformVarFileTypes.Inline;
  }
}
