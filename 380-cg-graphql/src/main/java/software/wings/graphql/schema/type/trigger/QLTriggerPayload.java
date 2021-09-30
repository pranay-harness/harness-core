package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;

@Scope(PermissionAttribute.ResourceType.SETTING)
@Builder
@OwnedBy(HarnessTeam.CDC)
public class QLTriggerPayload {
  String clientMutationId;
  QLTrigger trigger;
}
