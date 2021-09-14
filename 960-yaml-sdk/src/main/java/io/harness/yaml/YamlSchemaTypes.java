/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
