/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.validator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

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

@Documented
@Constraint(validatedBy = {NGVariableNameValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
@OwnedBy(CDC)
public @interface NGVariableName {
  String message() default "Variable name can only have a-z, A-Z, 0-9, _ and some reserved keywords are not allowed.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
