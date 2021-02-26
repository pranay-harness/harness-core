package io.harness.ng.core.event;

import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.UNRECOGNIZED;

import io.harness.EntityType;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;

import java.util.EnumMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventProtoToEntityHelper {
  public static Map<EntityTypeProtoEnum, EntityType> getEntityTypeProtoEnumToRestEnumMap() {
    Map<EntityTypeProtoEnum, EntityType> mappingBetweenProtoAndActualEnum = new EnumMap<>(EntityTypeProtoEnum.class);
    for (EntityTypeProtoEnum entityTypeProtoEnum : EntityTypeProtoEnum.values()) {
      // Handle any seprate enum here.
      if (entityTypeProtoEnum == UNRECOGNIZED) {
        continue;
      }
      mappingBetweenProtoAndActualEnum.put(entityTypeProtoEnum, EntityType.valueOf(entityTypeProtoEnum.name()));
    }
    return mappingBetweenProtoAndActualEnum;
  }

  public static EntityType getEntityTypeFromProto(EntityTypeProtoEnum entityTypeProtoEnum) {
    return EntityType.valueOf(entityTypeProtoEnum.name());
  }
}
