package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.provision.terraform.TerraformApplyStepParameters.TerraformApplyStepParametersBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
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
@OwnedBy(HarnessTeam.CDP)
@JsonTypeName(StepSpecTypeConstants.TERRAFORM_APPLY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformApplyStepInfo extends TerraformApplyBaseStepInfo implements CDStepInfo {
  @JsonProperty("configuration") TerrformStepConfiguration terrformStepConfiguration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformApplyStepInfo(String provisionerIdentifier, TerrformStepConfiguration terrformStepConfiguration) {
    super(provisionerIdentifier);
    this.terrformStepConfiguration = terrformStepConfiguration;
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
  public SpecParameters getSpecParameters() {
    TerraformApplyStepParametersBuilder builder = TerraformApplyStepParameters.infoBuilder();
    builder.provisionerIdentifier(provisionerIdentifier);

    TerraformStepConfigurationType stepConfigurationType =
        terrformStepConfiguration.getTerraformStepConfigurationType();
    builder.stepConfigurationType(stepConfigurationType);
    if (TerraformStepConfigurationType.INLINE == stepConfigurationType) {
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
      if (EmptyPredicate.isNotEmpty(terraformVarFiles)) {
        terraformVarFiles.forEach(varFile -> {
          TerraformVarFileSpec varFileSpec = varFile.getTerraformVarFileSpec();
          if (varFileSpec instanceof InlineTerraformVarFileSpec) {
            inlineVarFiles.add(((InlineTerraformVarFileSpec) varFileSpec).getContent().getValue());
          } else if (varFileSpec instanceof RemoteTerraformVarFileSpec) {
            remoteVarFiles.add(((RemoteTerraformVarFileSpec) varFileSpec).getStoreConfigWrapper());
          }
        });
      }
      if (EmptyPredicate.isNotEmpty(remoteVarFiles)) {
        builder.remoteVarFiles(remoteVarFiles);
      }
      if (EmptyPredicate.isNotEmpty(inlineVarFiles)) {
        builder.inlineVarFiles(ParameterField.createValueField(inlineVarFiles));
      }
    }

    return builder.build();
  }
}
