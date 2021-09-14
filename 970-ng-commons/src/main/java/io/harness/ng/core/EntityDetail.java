/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.ng.core.deserializer.EntityDetailDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "EntityDetailKeys")
@JsonDeserialize(using = EntityDetailDeserializer.class)
public class EntityDetail {
  EntityType type;
  EntityReference entityRef;
  String name;

  @Builder
  public EntityDetail(EntityType type, EntityReference entityRef, String name) {
    this.type = type;
    this.entityRef = entityRef;
    this.name = name;
  }
}
