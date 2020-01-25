package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Value;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;

@Value
public class QLUpdateUserGroupPermissionsInput {
  private String requestId;
  private String userGroupId;
  private QLUserGroupPermissions permissions;
}
