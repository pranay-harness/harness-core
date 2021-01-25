package io.harness.ng.core;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.ng.core.deserializer.EntityDetailDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
