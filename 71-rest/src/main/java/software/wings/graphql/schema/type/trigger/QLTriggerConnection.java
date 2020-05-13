package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLTriggerConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLTrigger> nodes;
}
