package software.wings.graphql.schema.mutation.secrets.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateSecretPayloadKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLUpdateSecretPayload implements QLMutationPayload {
  String clientMutationId;
  QLSecret secret;
}
