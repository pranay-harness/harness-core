package io.harness.pms.yaml.validation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetValidatorType;

import lombok.Value;

@Value
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.pms.yaml.validation.InputSetValidator")
public class InputSetValidator {
  InputSetValidatorType validatorType;
  // Content of the validator will be set here by Deserializer.
  String parameters;
}
