/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.event;

import io.harness.EntityType;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityToEntityProtoHelper {
  public static EntityTypeProtoEnum getEntityTypeFromProto(EntityType entityType) {
    for (EntityTypeProtoEnum value : EntityTypeProtoEnum.values()) {
      if (entityType.name().equalsIgnoreCase(value.name())) {
        return value;
      }
    }
    throw new InvalidRequestException("Invalid Entity Type");
  }
}
