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
@Constraint(validatedBy = {RoleFilterValidator.class})
@Target(TYPE)
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface ValidRoleFilter {
  String message() default "Invalid role filter";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
