package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;

@Value
public class QLUpdateUserGroupPermissionsInput implements QLMutationInput {
  private String clientMutationId;
  private String userGroupId;
  private QLUserGroupPermissions permissions;
}
