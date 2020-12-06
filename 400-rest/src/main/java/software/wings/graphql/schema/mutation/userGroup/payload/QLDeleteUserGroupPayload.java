package software.wings.graphql.schema.mutation.userGroup.payload;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteUserGroupPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLDeleteUserGroupPayload implements QLMutationPayload {
  String clientMutationId;
}
