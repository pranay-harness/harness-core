package software.wings.graphql.schema.mutation.userGroup.payload;

import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLRemoveUserFromUserGroupPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLRemoveUserFromUserGroupPayload {
  String clientMutationId;
  QLUserGroup userGroup;
}
