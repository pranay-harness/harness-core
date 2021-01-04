package io.harness.pms.yaml.validation;

import lombok.Value;

@Value
public class InputSetValidator {
  InputSetValidatorType validatorType;
  // Content of the validator will be set here by Deserializer.
  String parameters;
}
