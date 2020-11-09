package io.harness.data.validator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TagValidator implements ConstraintValidator<Tag, String> {
  public static final int TAG_MAX_LENGTH = 64;

  @Override
  public void initialize(Tag constraintAnnotation) {
    // Nothing to initialize
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return isValidTag(value);
  }

  // A static method added in case we need to do the same validation on some string w/o the annotation.
  public static boolean isValidTag(String value) {
    return isNotEmpty(value) && value.length() <= TAG_MAX_LENGTH;
  }
}