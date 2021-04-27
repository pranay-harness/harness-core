package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.provision.terraform.TerraformDestroyStepParameters.TerraformDestroyStepParametersBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.Visitable;
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
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@TypeAlias("terraformDestroyStepInfo")
@JsonTypeName(StepSpecTypeConstants.TERRAFORM_DESTROY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformDestroyStepInfo extends TerraformDestroyBaseStepInfo implements CDStepInfo, Visitable {
  @JsonProperty("configuration") TerrformStepConfiguration terrformStepConfiguration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformDestroyStepInfo(
      ParameterField<String> provisionerIdentifier, TerrformStepConfiguration terrformStepConfiguration) {
    super(provisionerIdentifier);
    this.terrformStepConfiguration = terrformStepConfiguration;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return TerraformDestroyStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(StepSpecTypeConstants.TERRAFORM_DESTROY).isPartOfFQN(false).build();
  }

  @Override
  public SpecParameters getSpecParameters() {
    TerraformDestroyStepParametersBuilder builder = TerraformDestroyStepParameters.infoBuilder();
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
      List<TerraformVarFileWrapper> terraformVarFiles = executionData.getTerraformVarFiles();
      if (EmptyPredicate.isNotEmpty(terraformVarFiles)) {
        terraformVarFiles.forEach(varFile -> {
          TerraformVarFileSpec varFileSpec = varFile.getVarFile().getSpec();
          if (varFileSpec instanceof InlineTerraformVarFileSpec) {
            inlineVarFiles.add(((InlineTerraformVarFileSpec) varFileSpec).getContent().getValue());
          } else if (varFileSpec instanceof RemoteTerraformVarFileSpec) {
            remoteVarFiles.add(((RemoteTerraformVarFileSpec) varFileSpec).getStoreConfigWrapper());
          }
        });
      }
      if (EmptyPredicate.isNotEmpty(remoteVarFiles)) {
        builder.remoteVarFileConfigs(remoteVarFiles);
      }
      if (EmptyPredicate.isNotEmpty(inlineVarFiles)) {
        builder.inlineVarFilesListContent(ParameterField.createValueField(inlineVarFiles));
      }
    }
    return builder.build();
  }
}
