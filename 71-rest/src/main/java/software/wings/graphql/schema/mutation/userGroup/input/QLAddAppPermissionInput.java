package software.wings.graphql.schema.mutation.userGroup.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.permissions.QLAppPermission;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddAppPermissionInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLAddAppPermissionInput {
  String clientMutationId;
  String userGroupId;
  QLAppPermission appPermission;
}
