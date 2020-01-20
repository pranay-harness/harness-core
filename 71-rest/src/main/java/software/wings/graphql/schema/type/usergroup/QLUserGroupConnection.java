package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
public class QLUserGroupConnection {
  private QLPageInfo pageInfo;
  @Singular private List<QLUserGroup> nodes;
}
