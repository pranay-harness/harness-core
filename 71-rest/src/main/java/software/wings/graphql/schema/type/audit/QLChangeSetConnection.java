package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLChangeSetConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLChangeSet> nodes;
}
