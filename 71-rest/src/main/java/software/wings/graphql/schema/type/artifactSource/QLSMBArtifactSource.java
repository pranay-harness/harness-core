package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSMBArtifactSourceKeys")
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLSMBArtifactSource implements QLArtifactSource {
  String name;
  String id;
  Long createdAt;
}
