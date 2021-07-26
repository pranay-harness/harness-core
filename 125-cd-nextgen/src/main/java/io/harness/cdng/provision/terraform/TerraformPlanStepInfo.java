package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
public class TerraformPlanStepInfo extends TerraformPlanBaseStepInfo implements CDStepInfo, WithConnectorRef {
  @NotNull @JsonProperty("configuration") TerraformPlanExecutionData terraformPlanExecutionData;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformPlanStepInfo(ParameterField<String> provisionerIdentifier,
      ParameterField<List<String>> delegateSelectors, TerraformPlanExecutionData terraformPlanExecutionData) {
    super(provisionerIdentifier, delegateSelectors);
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
    validateSpecParams();
    return TerraformPlanStepParameters.infoBuilder()
        .provisionerIdentifier(provisionerIdentifier)
        .delegateSelectors(delegateSelectors)
        .configuration(terraformPlanExecutionData.toStepParameters())
        .build();
  }

  void validateSpecParams() {
    Validator.notNullCheck("Terraform Plan configuration is NULL", terraformPlanExecutionData);
    terraformPlanExecutionData.validateParams();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    validateSpecParams();
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put("configuration.configFiles.store.spec.connectorRef",
        terraformPlanExecutionData.getTerraformConfigFilesWrapper().store.getSpec().getConnectorReference());

    List<TerraformVarFileWrapper> terraformVarFiles = terraformPlanExecutionData.getTerraformVarFiles();

    if (EmptyPredicate.isNotEmpty(terraformVarFiles)) {
      for (TerraformVarFileWrapper terraformVarFile : terraformVarFiles) {
        if (terraformVarFile.getVarFile().getType().equals(TerraformVarFileTypes.Remote)) {
          connectorRefMap.put(
              "configuration.varFiles." + terraformVarFile.getVarFile().identifier + ".spec.store.spec.connectorRef",
              ((RemoteTerraformVarFileSpec) terraformVarFile.varFile.spec).store.getSpec().getConnectorReference());
        }
      }
    }

    connectorRefMap.put("configuration.secretManagerRef", terraformPlanExecutionData.getSecretManagerRef());
    return connectorRefMap;
  }
}
