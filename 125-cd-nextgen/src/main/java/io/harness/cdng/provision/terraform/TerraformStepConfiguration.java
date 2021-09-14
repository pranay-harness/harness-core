/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.terraform.TerraformStepConfigurationParameters.TerraformStepConfigurationParametersBuilder;
import io.harness.validation.Validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformStepConfiguration")
public class TerraformStepConfiguration {
  @NotNull @JsonProperty("type") TerraformStepConfigurationType terraformStepConfigurationType;
  @JsonProperty("spec") TerraformExecutionData terraformExecutionData;

  public TerraformStepConfigurationParameters toStepParameters() {
    TerraformStepConfigurationParametersBuilder builder = TerraformStepConfigurationParameters.builder();
    validateParams();
    builder.type(terraformStepConfigurationType);
    if (terraformExecutionData != null) {
      builder.spec(terraformExecutionData.toStepParameters());
    }
    return builder.build();
  }

  void validateParams() {
    Validator.notNullCheck("Step Configuration Type is null", terraformStepConfigurationType);

    if (terraformStepConfigurationType == TerraformStepConfigurationType.INLINE) {
      Validator.notNullCheck("Spec inside Configuration cannot be null", terraformExecutionData);
      terraformExecutionData.validateParams();
    }
  }
}
