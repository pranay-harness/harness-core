package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Scope(PermissionAttribute.ResourceType.SERVICE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLArtifactoryProps extends QLObject {
  String getArtifactoryConnectorId();
  String getRepository();
}
