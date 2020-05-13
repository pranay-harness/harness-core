package software.wings.graphql.schema.type.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.SETTING)
@FieldNameConstants(innerTypeName = "QLArtifactKeys")
public class QLArtifact implements QLObject {
  private String id;
  private String buildNo;
  private Long collectedAt;
  private String artifactSourceId;
}
