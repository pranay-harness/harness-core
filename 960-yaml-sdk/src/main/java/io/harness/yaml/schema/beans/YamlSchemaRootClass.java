/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class YamlSchemaRootClass {
  EntityType entityType;

  Class<?> clazz;

  YamlSchemaMetadata yamlSchemaMetadata;

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
