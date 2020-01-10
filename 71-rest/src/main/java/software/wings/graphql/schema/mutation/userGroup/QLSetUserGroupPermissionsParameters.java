package software.wings.graphql.schema.mutation.userGroup;

import lombok.Value;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;

@Value
public class QLSetUserGroupPermissionsParameters {
  private String userGroupId;
  private QLUserGroupPermissions permissions;
}
