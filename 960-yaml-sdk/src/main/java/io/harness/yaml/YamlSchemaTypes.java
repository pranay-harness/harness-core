package io.harness.yaml;

import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YamlSchemaTypes {
  SupportedPossibleFieldTypes[] value();

  /**
   * Set a defaultType if it is default for schema. (It will appear as topmost suggestion.)
   */
  SupportedPossibleFieldTypes defaultType() default SupportedPossibleFieldTypes.none;
}
