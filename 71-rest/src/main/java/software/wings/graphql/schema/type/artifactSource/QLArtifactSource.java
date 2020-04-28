package software.wings.graphql.schema.type.artifactSource;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.SERVICE)
public interface QLArtifactSource extends QLObject {
  Long getCreatedAt();
  String getName();
  String getId();
}
