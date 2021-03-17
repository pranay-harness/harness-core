package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLRemoveUserFromUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLRemoveUserFromUserGroupInput {
  private String clientMutationId;
  private String userGroupId;
  private String userId;
}
