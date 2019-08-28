package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.SETTING)
public class QLTagsInUseConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLTagsInUse> nodes;
}
