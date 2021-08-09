package software.wings.graphql.schema.type.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.SETTING)
@FieldNameConstants(innerTypeName = "QLArtifactKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLArtifact implements QLObject {
  private String id;
  private String buildNo;
  private Long collectedAt;
  private String artifactSourceId;
}
