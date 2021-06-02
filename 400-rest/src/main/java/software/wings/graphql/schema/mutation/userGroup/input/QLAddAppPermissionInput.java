package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.permissions.QLAppPermission;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddAppPermissionInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAddAppPermissionInput {
  String clientMutationId;
  String userGroupId;
  QLAppPermission appPermission;
}
