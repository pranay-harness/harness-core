package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SERVICE)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLArtifactoryDockerProps implements QLArtifactoryProps {
  String artifactoryConnectorId;
  String repository;
  String dockerImageName;
  String dockerRepositoryServer;
}
