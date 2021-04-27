package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.provision.terraform.TerraformPlanStepParameters.TerraformPlanStepParametersBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.ngpipeline.common.ParameterFieldHelper;
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
@JsonTypeName(StepSpecTypeConstants.TERRAFORM_PLAN)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformPlanStepInfo extends TerraformPlanBaseStepInfo implements CDStepInfo {
  @JsonProperty("configuration") TerraformPlanExecutionData terraformPlanExecutionData;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformPlanStepInfo(
      ParameterField<String> provisionerIdentifier, TerraformPlanExecutionData terraformPlanExecutionData) {
    super(provisionerIdentifier);
    this.terraformPlanExecutionData = terraformPlanExecutionData;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return TerraformPlanStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    TerraformPlanStepParametersBuilder builder = TerraformPlanStepParameters.infoBuilder();
    builder.provisionerIdentifier(provisionerIdentifier);
    if (terraformPlanExecutionData != null) {
      builder.workspace(terraformPlanExecutionData.getWorkspace());
      builder.targets(terraformPlanExecutionData.getTargets());
      builder.environmentVariables(
          NGVariablesUtils.getMapOfVariables(terraformPlanExecutionData.getEnvironmentVariables(), 0L));
      TerraformBackendConfig backendConfig = terraformPlanExecutionData.getTerraformBackendConfig();
      if (backendConfig != null) {
        TerraformBackendConfigSpec backendConfigSpec = backendConfig.getTerraformBackendConfigSpec();
        if (backendConfigSpec instanceof InlineTerraformBackendConfigSpec) {
          builder.backendConfig(((InlineTerraformBackendConfigSpec) backendConfigSpec).getContent());
        }
      }
      builder.configFilesWrapper(terraformPlanExecutionData.getTerraformConfigFilesWrapper().getStoreConfigWrapper());
      List<StoreConfigWrapper> remoteVarFiles = new ArrayList<>();
      List<String> inlineVarFiles = new ArrayList<>();
      List<TerraformVarFileWrapper> terraformVarFiles = terraformPlanExecutionData.getTerraformVarFiles();
      if (EmptyPredicate.isNotEmpty(terraformVarFiles)) {
        terraformVarFiles.forEach(varFile -> {
          TerraformVarFileSpec varFileSpec = varFile.getVarFile().getSpec();
          if (varFileSpec instanceof InlineTerraformVarFileSpec) {
            inlineVarFiles.add(
                ParameterFieldHelper.getParameterFieldValue(((InlineTerraformVarFileSpec) varFileSpec).getContent()));
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
      builder.terraformPlanCommand(terraformPlanExecutionData.getCommand());
      builder.secretManagerId(terraformPlanExecutionData.getSecretManagerRef());
    }
    return builder.build();
  }
}
