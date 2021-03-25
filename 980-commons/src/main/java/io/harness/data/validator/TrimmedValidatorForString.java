package io.harness.data.validator;

import static io.harness.data.structure.HasPredicate.hasNone;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TrimmedValidatorForString implements ConstraintValidator<Trimmed, String> {
  @Override
  public void initialize(Trimmed constraintAnnotation) {
    // Nothing to initialize
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (hasNone(value)) {
      return true;
    }

    return !Character.isWhitespace(value.charAt(0)) && !Character.isWhitespace(value.charAt(value.length() - 1));
  }
}
