package software.wings.utils.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
public class FutureDateValidator implements ConstraintValidator<FutureDate, Long> {
  @Override
  public void initialize(FutureDate constraintAnnotation) {}

  @Override
  public boolean isValid(Long value, ConstraintValidatorContext context) {
    return value > System.currentTimeMillis();
  }
}
