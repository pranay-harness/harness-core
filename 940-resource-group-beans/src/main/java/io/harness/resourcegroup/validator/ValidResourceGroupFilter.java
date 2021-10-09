package io.harness.resourcegroup.validator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

@OwnedBy(HarnessTeam.PL)
@Documented
@Constraint(validatedBy = {ResourceGroupFilterValidator.class})
@Target(TYPE)
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface ValidResourceGroupFilter {
  String message() default "Invalid resource group filter";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
