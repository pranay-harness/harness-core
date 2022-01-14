package software.wings.graphql.schema.mutation.artifact;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class ArtifactCleanUpPayload {
  private String message;
}
