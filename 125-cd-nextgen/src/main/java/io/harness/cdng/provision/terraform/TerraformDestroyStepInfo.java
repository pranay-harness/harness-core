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
import io.harness.walktree.visitor.Visitable;

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
  public TerraformDestroyStepInfo(ParameterField<String> provisionerIdentifier,
      ParameterField<List<String>> delegateSelectors, TerrformStepConfiguration terrformStepConfiguration) {
    super(provisionerIdentifier, delegateSelectors);
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
  public SpecParameters getSpecParameters() {
    Validator.notNullCheck("Terraform Step configuration is null", terrformStepConfiguration);
    return TerraformDestroyStepParameters.infoBuilder()
        .provisionerIdentifier(provisionerIdentifier)
        .delegateSelectors(delegateSelectors)
        .configuration(terrformStepConfiguration.toStepParameters())
        .build();
  }
}
