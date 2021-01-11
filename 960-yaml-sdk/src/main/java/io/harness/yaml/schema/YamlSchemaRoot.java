package io.harness.yaml.schema;

import io.harness.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface YamlSchemaRoot {
  EntityType value();

  /**
   * If an entity is available at org level, a schema is prepared by removing <b>projectIdentifier</b> from schema.
   */
  boolean availableAtOrgLevel() default false;

  /**
   * If an entity is available at account level, a schema is prepared by removing <b>projectIdentifier</b> and
   * <b>orgIdentifier</b> from schema.
   */
  boolean availableAtAccountLevel() default false;

  /**
   * If an entity is available at project.
   */
  boolean availableAtProjectLevel() default true;
}
