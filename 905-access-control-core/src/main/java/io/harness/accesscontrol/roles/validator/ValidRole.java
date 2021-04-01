package io.harness.accesscontrol.roles.validator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@Documented
@Constraint(validatedBy = {RoleValidator.class})
@Target(TYPE)
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface ValidRole {
  String message() default "Invalid role";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
