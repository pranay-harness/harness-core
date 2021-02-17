package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLACRArtifactSourceKeys")
@Scope(PermissionAttribute.ResourceType.SERVICE)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLACRArtifactSource implements QLArtifactSource {
  String name;
  String id;
  Long createdAt;
  String azureCloudProviderId;
  String subscriptionId;
  String repositoryName;
  String registryName;
}
