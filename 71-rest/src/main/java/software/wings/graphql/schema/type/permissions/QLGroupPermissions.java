package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
@Scope(PermissionAttribute.ResourceType.USER) // Change the scope
public class QLGroupPermissions {
  QLAccountPermissions accountPermissions;
  List<QLAppPermissions> appPermissions; // Can have this as a set too
}
