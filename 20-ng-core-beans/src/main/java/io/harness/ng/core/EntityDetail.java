package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntityDetail {
  EntityType type;

  // EntityReference should match with the impl class defined in EntityType.
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes(value =
      {
        @JsonSubTypes.Type(value = IdentifierRef.class, name = "PROJECTS")
        , @JsonSubTypes.Type(value = IdentifierRef.class, name = "PIPELINES"),
            @JsonSubTypes.Type(value = IdentifierRef.class, name = "CONNECTORS"),
            @JsonSubTypes.Type(value = IdentifierRef.class, name = "SECRETS"),
            @JsonSubTypes.Type(value = IdentifierRef.class, name = "SERVICE"),
            @JsonSubTypes.Type(value = IdentifierRef.class, name = "ENVIRONMENT"),
            @JsonSubTypes.Type(value = InputSetReference.class, name = "INPUT_SETS")
      })
  EntityReference entityRef;
  String name;

  @Builder
  public EntityDetail(EntityType type, EntityReference entityRef, String name) {
    this.type = type;
    this.entityRef = entityRef;
    this.name = name;
  }
}
