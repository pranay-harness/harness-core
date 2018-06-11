package io.harness.data.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EntityNameValidator implements ConstraintValidator<EntityName, String> {
  private static final Set<Character> ALLOWED_CHARS_SET =
      Sets.newHashSet(Lists.charactersOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ "));

  @Override
  public void initialize(EntityName constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return isValid(value);
  }

  // A static method added in case we need to do the same validation on some string w/o the annotation.
  public static boolean isValid(String value) {
    if (isEmpty(value)) {
      return true;
    }
    return ALLOWED_CHARS_SET.containsAll(Sets.newHashSet(Lists.charactersOf(value)));
  }

  /**
   * A central place where the logic used to migrate older names to the new names would be kept.
   * Must be completely kept in this class.
   */
  public static String getMappedString(String string) {
    if (isEmpty(string)) {
      return string;
    }
    StringBuilder sb = new StringBuilder();
    Lists.charactersOf(string).forEach(ch -> sb.append(ALLOWED_CHARS_SET.contains(ch) ? ch : '-'));
    return sb.toString();
  }
}