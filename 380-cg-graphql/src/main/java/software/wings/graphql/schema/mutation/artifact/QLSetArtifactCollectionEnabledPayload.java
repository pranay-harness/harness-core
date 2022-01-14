package software.wings.graphql.schema.mutation.artifact;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLSetArtifactCollectionEnabledPayload {
  String clientMutationId;
  String message;
}
