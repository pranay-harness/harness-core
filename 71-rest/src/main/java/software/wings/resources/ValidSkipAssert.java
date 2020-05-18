package software.wings.resources;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@Documented
@Constraint(validatedBy = {ValidSkipAssertValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface ValidSkipAssert {
  String message() default "Expression Invalid";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
