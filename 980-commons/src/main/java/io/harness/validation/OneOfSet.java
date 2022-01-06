/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.validation;

import static java.lang.annotation.ElementType.TYPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
@OwnedBy(HarnessTeam.CDC)
public @interface OneOfSet {
  /**
   * Provide set of sets out of which one only set is required in schema validation.
   * If there are multiple fields in a single set, provide them as a single comma separated string.
   *
   * Eg, say we want either fields set {a, b, c} comes or fields set {e, f} comes, provide fields as:
   * fields = {"a, b, c", "e, f"}
   */
  String[] fields() default {};

  /**
   * Provide java field names which are to marked required in schema.
   * This is needed because there might be few fields on which we can't put @NotNull annotation as they could be in
   * oneOf sets.
   */
  String[] requiredFieldNames() default {};
}
