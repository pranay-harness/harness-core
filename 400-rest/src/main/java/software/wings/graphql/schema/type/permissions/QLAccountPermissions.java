package software.wings.graphql.schema.type.permissions;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAccountPermissionsKeys")
public class QLAccountPermissions {
  private Set<QLAccountPermissionType> accountPermissionTypes;
}
