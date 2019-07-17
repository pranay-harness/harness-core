package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApplicationKeys")
@Scope(ResourceType.APPLICATION)
public class QLApplication implements QLObject {
  String id;
  String name;
  String description;
  Long createdAt;
  QLUser createdBy;
}
