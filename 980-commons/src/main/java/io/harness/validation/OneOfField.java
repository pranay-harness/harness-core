/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

  /**
   * Fields of which one is required. This should be same as in java pojo.
   */
  String[] fields() default {};

  /**
   * Set to true, if fields in the list are all allowed to be null together.
   * In short, if we have a condition that either 0 or 1 among fields must be present we should set it to true.
   * Currently <b>not</b> supported in YAML Schema autogeneration.
   */
  boolean nullable() default false;
}
