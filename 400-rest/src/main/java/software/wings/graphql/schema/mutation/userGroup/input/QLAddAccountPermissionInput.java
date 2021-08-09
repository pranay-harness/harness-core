package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddAccountPermissionInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAddAccountPermissionInput {
  private String clientMutationId;
  private String userGroupId;
  private QLAccountPermissionType accountPermission;
}
