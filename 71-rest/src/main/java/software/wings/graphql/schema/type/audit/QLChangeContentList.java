package software.wings.graphql.schema.type.audit;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLChangeContentList {
  private List<QLChangeContent> data;
}
