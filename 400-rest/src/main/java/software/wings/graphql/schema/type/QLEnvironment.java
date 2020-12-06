package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvironmentKeys")
@Scope(ResourceType.APPLICATION)
public class QLEnvironment implements QLObject {
  private String id;
  private String name;
  private String description;
  private QLEnvironmentType type;
  private Long createdAt;
  private QLUser createdBy;
  private String appId;
}
