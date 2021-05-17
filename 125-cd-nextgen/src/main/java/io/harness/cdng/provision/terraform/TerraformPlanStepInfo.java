package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
    Validator.notNullCheck("Terraform Plan configuration is NULL", terraformPlanExecutionData);
    return TerraformPlanStepParameters.infoBuilder()
        .provisionerIdentifier(provisionerIdentifier)
        .delegateSelectors(delegateSelectors)
        .configuration(terraformPlanExecutionData.toStepParameters())
        .build();
  }
}
