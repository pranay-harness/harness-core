package software.wings.utils.validation;

import java.util.Calendar;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
    return value == null || value.longValue() >= getDateInMillis();
  }

  public static long getDateInMillis() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.MILLISECOND, 0);
    c.set(Calendar.SECOND, 0);
    return c.getTimeInMillis();
  }
}
