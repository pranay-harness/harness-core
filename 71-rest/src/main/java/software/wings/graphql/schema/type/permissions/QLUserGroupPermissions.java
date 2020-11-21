package software.wings.graphql.schema.type.permissions;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
public class QLUserGroupPermissions {
  QLAccountPermissions accountPermissions;
  List<QLAppPermission> appPermissions;
}
