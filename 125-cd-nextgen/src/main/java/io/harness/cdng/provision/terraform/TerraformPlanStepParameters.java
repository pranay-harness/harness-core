package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.common.SpecParameters;
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

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TerraformPlanStepParameters extends TerraformPlanBaseStepInfo implements SpecParameters {
  String name;
  String identifier;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> workspace;
  StoreConfigWrapper configFilesWrapper;
  List<StoreConfigWrapper> remoteVarFiles;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> inlineVarFiles;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> backendConfig;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> targets;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  Map<String, Object> environmentVariables;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> secretManagerId;
  TerraformPlanCommand terraformPlanCommand;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformPlanStepParameters(ParameterField<String> provisionerIdentifier, String name, String identifier,
      ParameterField<String> workspace, StoreConfigWrapper configFilesWrapper, List<StoreConfigWrapper> remoteVarFiles,
      ParameterField<List<String>> inlineVarFiles, ParameterField<String> backendConfig,
      ParameterField<List<String>> targets, ParameterField<String> timeout, Map<String, Object> environmentVariables,
      ParameterField<String> secretManagerId, TerraformPlanCommand terraformPlanCommand) {
    super(provisionerIdentifier);
    this.name = name;
    this.identifier = identifier;
    this.workspace = workspace;
    this.configFilesWrapper = configFilesWrapper;
    this.remoteVarFiles = remoteVarFiles;
    this.inlineVarFiles = inlineVarFiles;
    this.backendConfig = backendConfig;
    this.targets = targets;
    this.timeout = timeout;
    this.environmentVariables = environmentVariables;
    this.secretManagerId = secretManagerId;
    this.terraformPlanCommand = terraformPlanCommand;
  }
}
