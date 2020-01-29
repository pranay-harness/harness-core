package software.wings.graphql.schema.mutation.userGroup.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateUserGroupPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserGroupPayload implements QLMutationPayload {
  String clientMutationId;
  QLUserGroup userGroup;
}