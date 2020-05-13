package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLWorkflowConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLWorkflow> nodes;
}
