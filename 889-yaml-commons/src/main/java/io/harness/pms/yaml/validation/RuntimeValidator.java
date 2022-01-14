package io.harness.pms.yaml.validation;

public interface RuntimeValidator {
  RuntimeValidatorResponse isValidValue(Object currentValue, String parameters);
}
