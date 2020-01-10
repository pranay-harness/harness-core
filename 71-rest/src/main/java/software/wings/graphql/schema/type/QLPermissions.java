package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.permissions.QLAccountPermissions;
import software.wings.graphql.schema.type.permissions.QLAppPermissions;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLPermissions implements QLObject {
  private QLAccountPermissions accountPermission;
  private QLAppPermissions appPermissions;
}
