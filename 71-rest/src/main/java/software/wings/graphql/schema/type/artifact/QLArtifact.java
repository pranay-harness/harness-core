package software.wings.graphql.schema.type.artifact;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

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
