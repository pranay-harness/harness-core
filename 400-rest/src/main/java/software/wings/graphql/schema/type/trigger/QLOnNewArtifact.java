package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Getter
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLOnNewArtifactKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLOnNewArtifact implements QLTriggerCondition {
  private QLTriggerConditionType triggerConditionType;
  private String artifactSourceId;
  private String artifactSourceName;
  private String artifactFilter;
  private Boolean regex;
}
