package software.wings.graphql.schema.type.artifactSource;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAMIArtifactSourceKeys")
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLAMIArtifactSource implements QLArtifactSource {
  String name;
  String id;
  Long createdAt;
}
