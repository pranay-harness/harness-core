/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.validator;

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
@Constraint(validatedBy = {UuidValidator.class})
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface Uuid {
  String message() default "Invalid UUID / Base64-encoded UUID";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
