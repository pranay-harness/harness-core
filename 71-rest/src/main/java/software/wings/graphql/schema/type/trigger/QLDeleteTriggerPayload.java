package software.wings.graphql.schema.type.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteTriggerPayload implements QLMutationPayload {
  String clientMutationId;
}
