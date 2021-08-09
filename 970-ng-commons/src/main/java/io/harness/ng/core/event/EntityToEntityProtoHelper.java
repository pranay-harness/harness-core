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
