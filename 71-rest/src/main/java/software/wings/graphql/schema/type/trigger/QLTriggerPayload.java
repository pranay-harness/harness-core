package software.wings.graphql.schema.type.trigger;

import lombok.Builder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.SETTING)
@Builder
public class QLTriggerPayload {
  String clientMutationId;
  QLTrigger trigger;
}
