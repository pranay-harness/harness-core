package software.wings.graphql.schema.type.trigger;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
@FieldNameConstants(innerTypeName = "QLTriggerKeys")
public class QLTrigger implements QLObject {
  private String id;
  private String name;
  private String description;
  private Long createdAt;
  private QLUser createdBy;
}
