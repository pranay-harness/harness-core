/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
