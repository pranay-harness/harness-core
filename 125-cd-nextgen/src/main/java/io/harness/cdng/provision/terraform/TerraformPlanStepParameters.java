package io.harness.cdng.provision.terraform;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
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
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformPlanStepParameters")
public class TerraformPlanStepParameters extends TerraformPlanBaseStepInfo implements SpecParameters {
  TerraformPlanExecutionDataParameters configuration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformPlanStepParameters(ParameterField<String> provisionerIdentifier,
      ParameterField<List<String>> delegateSelectors, TerraformPlanExecutionDataParameters configuration) {
    super(provisionerIdentifier, delegateSelectors);
    this.configuration = configuration;
  }
}
