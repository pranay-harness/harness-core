/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@OwnedBy(DX)
public @interface YamlSchemaTypes {
  SupportedPossibleFieldTypes[] value();

  /**
   * Set a defaultType if it is default for schema. (It will appear as topmost suggestion.)
   */
  SupportedPossibleFieldTypes defaultType() default SupportedPossibleFieldTypes.none;

  /**
   * Set a regex which will be used only in case of SupportedPossibleFieldTypes.string
   */
  String pattern() default "";

  /**
   * Set min string length which will be used only in case of SupportedPossibleFieldTypes.string
   */
  int minLength() default - 1;
}
