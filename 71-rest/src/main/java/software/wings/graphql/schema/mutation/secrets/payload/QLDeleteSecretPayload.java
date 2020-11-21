package software.wings.graphql.schema.mutation.secrets.payload;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteSecretPayloadKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteSecretPayload {
  String clientMutationId;
}
