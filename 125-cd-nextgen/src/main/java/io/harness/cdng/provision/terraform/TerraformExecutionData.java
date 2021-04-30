package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformExecutionData {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> workspace;
  @JsonProperty("configFiles") TerraformConfigFilesWrapper terraformConfigFilesWrapper;
  @JsonProperty("varFiles") List<TerraformVarFileWrapper> terraformVarFiles;
  @JsonProperty("backendConfig") TerraformBackendConfig terraformBackendConfig;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> targets;
  List<NGVariable> environmentVariables;
}
