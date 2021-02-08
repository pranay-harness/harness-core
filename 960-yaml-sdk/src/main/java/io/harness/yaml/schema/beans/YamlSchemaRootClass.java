package io.harness.yaml.schema.beans;

import io.harness.EntityType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YamlSchemaRootClass {
  EntityType entityType;

  Class<?> clazz;

  /**
   * If an entity is available at org level, a schema is prepared by removing <b>projectIdentifier</b> from schema.
   */
  boolean availableAtOrgLevel;

  /**
   * If an entity is available at account level, a schema is prepared by removing <b>projectIdentifier</b> and
   * <b>orgIdentifier</b> from schema.
   */
  boolean availableAtAccountLevel;

  /**
   * If an entity is available at project.
   */
  @Builder.Default boolean availableAtProjectLevel = true;
}
