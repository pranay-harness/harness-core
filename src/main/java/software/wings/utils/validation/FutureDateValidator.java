package software.wings.utils.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
public class FutureDateValidator implements ConstraintValidator<FutureDate, Long> {
  /* (non-Javadoc)
   * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
   */
  @Override
  public void initialize(FutureDate constraintAnnotation) {}

  /* (non-Javadoc)
   * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
   */
  @Override
  public boolean isValid(Long value, ConstraintValidatorContext context) {
    return value > System.currentTimeMillis();
  }
}
