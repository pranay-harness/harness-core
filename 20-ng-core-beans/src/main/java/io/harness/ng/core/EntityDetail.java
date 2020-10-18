package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntityDetail {
  EntityType type;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true, defaultImpl = IdentifierRef.class)
  EntityReference entityRef;
  String name;

  @Builder
  public EntityDetail(EntityType type, EntityReference entityRef, String name) {
    this.type = type;
    this.entityRef = entityRef;
    this.name = name;
  }
}
