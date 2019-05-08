package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLInstanceCount {
  private int count;
  private QLInstanceCountType instanceCountType;
}
