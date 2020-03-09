package software.wings.graphql.schema.mutation.secrets.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeleteSecretInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteSecretInput {
  String clientMutationId;
  String secretId;
  QLSecretType secretType;
}
