package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
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
public class TerraformDestroyStepParameters extends TerraformDestroyBaseStepInfo implements SpecParameters {
  String name;
  String identifier;
  TerraformStepConfigurationType stepConfigurationType;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> workspace;
  StoreConfigWrapper configFilesWrapper;
  List<StoreConfigWrapper> remoteVarFileConfigs;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> inlineVarFilesListContent;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> backendConfig;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> targets;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  Map<String, Object> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformDestroyStepParameters(ParameterField<String> provisionerIdentifier, String name, String identifier,
      TerraformStepConfigurationType stepConfigurationType, ParameterField<String> workspace,
      StoreConfigWrapper configFilesWrapper, List<StoreConfigWrapper> remoteVarFileConfigs,
      ParameterField<List<String>> inlineVarFilesListContent, ParameterField<String> backendConfig,
      ParameterField<List<String>> targets, ParameterField<String> timeout, Map<String, Object> environmentVariables) {
    super(provisionerIdentifier);
    this.name = name;
    this.identifier = identifier;
    this.stepConfigurationType = stepConfigurationType;
    this.workspace = workspace;
    this.configFilesWrapper = configFilesWrapper;
    this.remoteVarFileConfigs = remoteVarFileConfigs;
    this.inlineVarFilesListContent = inlineVarFilesListContent;
    this.backendConfig = backendConfig;
    this.targets = targets;
    this.timeout = timeout;
    this.environmentVariables = environmentVariables;
  }
}
