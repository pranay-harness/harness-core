package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.provision.terraform.TerraformApplyStepParameters.TerraformApplyStepParametersBuilder;
import static io.harness.cdng.provision.terraform.TerraformStepConfigurationType.INLINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_APPLY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.utils.NGVariablesUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@JsonTypeName(TERRAFORM_APPLY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformApplyStepInfo extends TerraformApplyBaseStepInfo implements CDStepInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  @JsonProperty("configuration") TerrformStepConfiguration terrformStepConfiguration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformApplyStepInfo(String provisionerIdentifier, String name, String identifier,
      TerrformStepConfiguration terrformStepConfiguration) {
    super(provisionerIdentifier);
    this.name = name;
    this.identifier = identifier;
    this.terrformStepConfiguration = terrformStepConfiguration;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return TerraformApplyStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    TerraformApplyStepParametersBuilder builder = TerraformApplyStepParameters.infoBuilder();
    builder.name(baseStepParameterInfo.getName());
    builder.identifier(baseStepParameterInfo.getIdentifier());
    builder.provisionerIdentifier(provisionerIdentifier);

    TerraformStepConfigurationType stepConfigurationType =
        terrformStepConfiguration.getTerraformStepConfigurationType();
    builder.stepConfigurationType(stepConfigurationType);
    if (INLINE == stepConfigurationType) {
      TerraformExecutionData executionData = terrformStepConfiguration.getTerraformExecutionData();
      builder.workspace(executionData.getWorkspace());
      builder.targets(executionData.getTargets());
      builder.environmentVariables(NGVariablesUtils.getMapOfVariables(executionData.getEnvironmentVariables(), 0L));
      TerraformBackendConfig backendConfig = executionData.getTerraformBackendConfig();
      if (backendConfig != null) {
        TerraformBackendConfigSpec backendConfigSpec = backendConfig.getTerraformBackendConfigSpec();
        if (backendConfigSpec instanceof InlineTerraformBackendConfigSpec) {
          builder.backendConfig(((InlineTerraformBackendConfigSpec) backendConfigSpec).getContent());
        }
      }
      builder.configFilesWrapper(executionData.getTerraformConfigFilesWrapper().getStoreConfigWrapper());
      List<StoreConfigWrapper> remoteVarFiles = new ArrayList<>();
      List<String> inlineVarFiles = new ArrayList<>();
      List<TerraformVarFile> terraformVarFiles = executionData.getTerraformVarFiles();
      if (isNotEmpty(terraformVarFiles)) {
        terraformVarFiles.forEach(varFile -> {
          TerraformVarFileSpec varFileSpec = varFile.getTerraformVarFileSpec();
          if (varFileSpec instanceof InlineTerraformVarFileSpec) {
            inlineVarFiles.add(((InlineTerraformVarFileSpec) varFileSpec).getContent().getValue());
          } else if (varFileSpec instanceof RemoteTerraformVarFileSpec) {
            remoteVarFiles.add(((RemoteTerraformVarFileSpec) varFileSpec).getStoreConfigWrapper());
          }
        });
      }
      if (isNotEmpty(remoteVarFiles)) {
        builder.remoteVarFiles(remoteVarFiles);
      }
      if (isNotEmpty(inlineVarFiles)) {
        builder.inlineVarFiles(ParameterField.createValueField(inlineVarFiles));
      }
    }

    return builder.build();
  }
}
