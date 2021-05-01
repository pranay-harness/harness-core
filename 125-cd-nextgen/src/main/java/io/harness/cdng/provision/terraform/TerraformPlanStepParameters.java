package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class TerraformPlanStepParameters extends TerraformPlanBaseStepInfo implements SpecParameters {
  TerraformPlanExecutionDataParameters configuration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformPlanStepParameters(
      ParameterField<String> provisionerIdentifier, TerraformPlanExecutionDataParameters configuration) {
    super(provisionerIdentifier);
    this.configuration = configuration;
  }
}
