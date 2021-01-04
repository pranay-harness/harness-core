package software.wings.graphql.schema.type.trigger;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;

@Scope(PermissionAttribute.ResourceType.SETTING)
@Builder
public class QLTriggerPayload {
  String clientMutationId;
  QLTrigger trigger;
}
