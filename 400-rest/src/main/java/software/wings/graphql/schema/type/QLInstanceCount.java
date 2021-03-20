package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLInstanceCount {
  private int count;
  private QLInstanceCountType instanceCountType;
}
