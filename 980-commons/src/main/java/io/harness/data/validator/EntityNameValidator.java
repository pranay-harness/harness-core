package io.harness.data.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EntityNameValidator implements ConstraintValidator<EntityName, String> {
  private static final String ALLOWED_CHARS_STRING_DEFAULT =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ";
  private static final Set<Character> ALLOWED_CHARS_SET_DEFAULT =
      Sets.newHashSet(Lists.charactersOf(ALLOWED_CHARS_STRING_DEFAULT));

  public static final String ALLOWED_CHARS_SERVICE_VARIABLE_STRING =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
  public static final String ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE =
      "Service Variable name can only have a-z, A-Z, 0-9, - and _";

  @Override
  public void initialize(EntityName constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (isEmpty(value)) {
      return true;
    }
    String allowedChars = (String) ((ConstraintValidatorContextImpl) context)
                              .getConstraintDescriptor()
                              .getAttributes()
                              .get("charSetString");
    if (isEmpty(allowedChars)) {
      allowedChars = ALLOWED_CHARS_STRING_DEFAULT;
    }
    return Sets.newHashSet(Lists.charactersOf(allowedChars)).containsAll(Lists.charactersOf(value));
  }

  // A static method added in case we need to do the same validation on some string w/o the annotation.
  public static boolean isValid(String value) {
    if (isEmpty(value)) {
      return true;
    }
    return ALLOWED_CHARS_SET_DEFAULT.containsAll(Sets.newHashSet(Lists.charactersOf(value)));
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
    Lists.charactersOf(string).forEach(ch -> sb.append(ALLOWED_CHARS_SET_DEFAULT.contains(ch) ? ch : '-'));
    return sb.toString();
  }
}