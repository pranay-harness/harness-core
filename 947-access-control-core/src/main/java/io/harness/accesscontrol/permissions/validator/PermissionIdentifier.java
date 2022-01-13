/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
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
@Constraint(validatedBy = {PermissionIdentifierValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
public @interface PermissionIdentifier {
  String message() default "Invalid identifier";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
