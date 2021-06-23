package software.wings.graphql.schema.mutation.artifact;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class ArtifactCleanUpPayload {
  private String message;
}
