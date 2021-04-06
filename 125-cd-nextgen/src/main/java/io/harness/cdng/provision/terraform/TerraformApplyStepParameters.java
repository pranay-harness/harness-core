package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TerraformApplyStepParameters extends TerraformApplyBaseStepInfo implements StepParameters {
  String name;
  String identifier;
  TerraformStepConfigurationType stepConfigurationType;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> workspace;
  StoreConfigWrapper configFilesWrapper;
  List<StoreConfigWrapper> remoteVarFiles;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> inlineVarFiles;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> backendConfig;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> targets;
  Map<String, Object> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformApplyStepParameters(String provisionerIdentifier, String name, String identifier,
      TerraformStepConfigurationType stepConfigurationType, ParameterField<String> workspace,
      StoreConfigWrapper configFilesWrapper, List<StoreConfigWrapper> remoteVarFiles,
      ParameterField<List<String>> inlineVarFiles, ParameterField<String> backendConfig,
      ParameterField<List<String>> targets, Map<String, Object> environmentVariables) {
    super(provisionerIdentifier);
    this.name = name;
    this.identifier = identifier;
    this.stepConfigurationType = stepConfigurationType;
    this.workspace = workspace;
    this.configFilesWrapper = configFilesWrapper;
    this.remoteVarFiles = remoteVarFiles;
    this.inlineVarFiles = inlineVarFiles;
    this.backendConfig = backendConfig;
    this.targets = targets;
    this.environmentVariables = environmentVariables;
  }
}
