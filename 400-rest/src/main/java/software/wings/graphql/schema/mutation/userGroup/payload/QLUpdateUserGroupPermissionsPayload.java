package software.wings.graphql.schema.mutation.userGroup.payload;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "UpdateUserGroupPermissionsPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLUpdateUserGroupPermissionsPayload implements QLMutationPayload {
  String clientMutationId;
  QLGroupPermissions permissions;
}
