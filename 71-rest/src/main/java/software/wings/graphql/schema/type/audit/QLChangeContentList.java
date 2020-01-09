package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLChangeContentList {
  private List<QLChangeContent> data;
}