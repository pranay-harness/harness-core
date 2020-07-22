package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLArtifactoryDockerProps implements QLArtifactoryProps {
  String artifactoryConnectorId;
  String repository;
  String dockerImageName;
  String dockerRepositoryServer;
}
