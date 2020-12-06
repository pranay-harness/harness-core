package software.wings.graphql.schema.type.user;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.USER)
public class QLUserConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLUser> nodes;
}
