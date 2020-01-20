package software.wings.graphql.schema.mutation.userGroup.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteUserGroupPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLDeleteUserGroupPayload implements QLObject {
  QLDeleteStatus status;
  String message;
}
