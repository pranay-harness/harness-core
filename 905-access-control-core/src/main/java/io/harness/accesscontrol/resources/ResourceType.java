package io.harness.accesscontrol.resources;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ResourceTypeKeys")
@Document("resourceTypes")
@TypeAlias("resourceTypes")
public class ResourceType {
  @Id String id;
  @NotEmpty String identifier;
  @NotEmpty String displayName;
}