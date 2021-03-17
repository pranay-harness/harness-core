package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;

import lombok.Value;

@Value
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUpdateUserGroupPermissionsInput implements QLMutationInput {
  private String clientMutationId;
  private String userGroupId;
  private QLUserGroupPermissions permissions;
}
