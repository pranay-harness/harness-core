package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.SETTING)
public class QLCustomCommitDetails {
  private String authorName;
  private String authorEmailId;
  private String commitMessage;
}
