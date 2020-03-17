package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
public class QLUserGroupPermissions {
  QLAccountPermissions accountPermissions;
  List<QLAppPermission> appPermissions;
}
