package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.SETTING)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCustomCommitDetails {
  private String authorName;
  private String authorEmailId;
  private String commitMessage;
}
