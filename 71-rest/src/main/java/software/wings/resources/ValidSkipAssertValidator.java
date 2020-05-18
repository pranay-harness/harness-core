package software.wings.resources;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
@Singleton
public class ValidSkipAssertValidator implements ConstraintValidator<ValidSkipAssert, String> {
  public static final String VARIABLE_PATTERN = "\\$\\{(.+)}";
  public static final String GROUP_1 = "$1";
  private final JexlEngine engine = new JexlBuilder().create();

  @Override
  public void initialize(ValidSkipAssert constraintAnnotation) {
    /* No Initialization Required */
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    try {
      String sanitized = value.replaceAll(VARIABLE_PATTERN, GROUP_1);
      engine.createExpression(sanitized);
    } catch (Exception e) {
      logger.error("Nothing to See here", e);
      return false;
    }
    return true;
  }
}
