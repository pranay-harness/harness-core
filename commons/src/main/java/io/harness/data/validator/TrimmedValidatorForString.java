package io.harness.data.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TrimmedValidatorForString implements ConstraintValidator<Trimmed, String> {
  @Override
  public void initialize(Trimmed constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (isEmpty(value)) {
      return true;
    }

    return !Character.isWhitespace(value.charAt(0)) && !Character.isWhitespace(value.charAt(value.length() - 1));
  }
}
