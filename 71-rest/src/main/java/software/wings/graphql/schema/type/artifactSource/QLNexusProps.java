package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Scope(PermissionAttribute.ResourceType.SERVICE)
public interface QLNexusProps extends QLObject {
  String getNexusConnectorId();
  String getRepository();
  QLNexusRepositoryFormat getRepositoryFormat();
}
