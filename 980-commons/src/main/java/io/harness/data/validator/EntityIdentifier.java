package io.harness.data.validator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@OwnedBy(PL)
@Documented
@Constraint(validatedBy = {EntityIdentifierValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface EntityIdentifier {
  String message() default "Invalid Identifier";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  boolean allowBlank() default false;
  boolean allowScoped() default false;
}
