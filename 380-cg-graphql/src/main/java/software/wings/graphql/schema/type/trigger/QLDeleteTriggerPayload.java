package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@OwnedBy(HarnessTeam.CDC)
public class QLDeleteTriggerPayload implements QLMutationPayload {
  String clientMutationId;
}
