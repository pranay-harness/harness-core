package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.terraform.TerraformStepConfigurationParameters.TerraformStepConfigurationParametersBuilder;
import io.harness.validation.Validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class TerraformStepConfiguration {
  @JsonProperty("type") TerraformStepConfigurationType terraformStepConfigurationType;
  @JsonProperty("spec") TerraformExecutionData terraformExecutionData;

  public TerraformStepConfigurationParameters toStepParameters() {
    TerraformStepConfigurationParametersBuilder builder = TerraformStepConfigurationParameters.builder();
    Validator.notNullCheck("Step Configuration Type is null", terraformStepConfigurationType);
    builder.type(terraformStepConfigurationType);
    if (terraformExecutionData != null) {
      builder.spec(terraformExecutionData.toStepParameters());
    }
    return builder.build();
  }
}
