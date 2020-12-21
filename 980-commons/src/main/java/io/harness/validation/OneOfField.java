package io.harness.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {OneOfFieldValidator.class})
@Documented
@ReportAsSingleViolation
@Repeatable(OneOfFields.class)
public @interface OneOfField {
  String message() default "{io.harness.validation.OneOfField.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
  String[] fields() default {};
  boolean nullable() default false;
}
