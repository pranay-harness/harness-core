package software.wings.graphql.schema.mutation.secretManager;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLUpsertSecretManagerPayload implements QLMutationPayload {
  String clientMutationId;
  QLSecretManager secretManager;
}
