package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformApplyStepParameters")
public class TerraformApplyStepParameters extends TerraformApplyBaseStepInfo implements SpecParameters {
  @NonNull TerraformStepConfigurationParameters configuration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformApplyStepParameters(ParameterField<String> provisionerIdentifier,
      ParameterField<List<String>> delegateSelectors, @NonNull TerraformStepConfigurationParameters configuration) {
    super(provisionerIdentifier, delegateSelectors);
    this.configuration = configuration;
  }
}
