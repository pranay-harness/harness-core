package software.wings.utils.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureDateValidator.class)
public @interface FutureDate {
  /**
   * Message.
   *
   * @return Validation message or key to show.
   */
  String message() default "{javax.validation.constraints.Future.message}";

  /**
   * Groups.
   *
   * @return List of groups on which this validation is part of.
   */
  Class<?>[] groups() default {};

  /**
   * Payload.
   *
   * @return validation payload.
   */
  Class<? extends Payload>[] payload() default {};
}
