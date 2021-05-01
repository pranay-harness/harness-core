package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

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
public class TerraformApplyStepParameters extends TerraformApplyBaseStepInfo implements SpecParameters {
  @NonNull TerrformStepConfigurationParameters configuration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformApplyStepParameters(
      ParameterField<String> provisionerIdentifier, TerrformStepConfigurationParameters configuration) {
    super(provisionerIdentifier);
    this.configuration = configuration;
  }
}
