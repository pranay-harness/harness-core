package software.wings.graphql.schema.mutation.userGroup.input;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;

import lombok.Value;

@Value
public class QLUpdateUserGroupPermissionsInput implements QLMutationInput {
  private String clientMutationId;
  private String userGroupId;
  private QLUserGroupPermissions permissions;
}
