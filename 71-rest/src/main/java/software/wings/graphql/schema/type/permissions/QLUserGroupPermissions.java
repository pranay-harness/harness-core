package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
public class QLUserGroupPermissions {
  Set<QLAccountPermissionType> accountPermissions;
  List<QLAppPermissions> appPermissions; // Can have this as a set too
}
