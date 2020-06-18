package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Getter
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLOnNewArtifactKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLGitHubEvent {
  private QLGitHubEventType event;
  private QLGitHubAction action;
}
