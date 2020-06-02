package io.harness.data.validator;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EntityIdentifierValidator implements ConstraintValidator<EntityIdentifier, String> {
  // Min Length : 3 characters
  // Max Length : 64 characters
  // Chars Allowed : Alphanumeric, Hyphen, Underscore
  // Should Start & End with Alphanumeric characters
  private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9][\\w-_]{1,62}[a-zA-Z0-9]$");

  @Override
  public void initialize(EntityIdentifier constraintAnnotation) {
    // Nothing to initialize
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return isValidEntityIdentifier(value);
  }

  // A static method added in case we need to do the same validation on some string w/o the annotation.
  public static boolean isValidEntityIdentifier(String value) {
    return value != null && pattern.matcher(value).matches();
  }
}