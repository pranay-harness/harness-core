package io.harness.ngpipeline.inputset.validators;

public interface RuntimeValidator {
  RuntimeValidatorResponse isValidValue(Object currentValue, String parameters);
}
