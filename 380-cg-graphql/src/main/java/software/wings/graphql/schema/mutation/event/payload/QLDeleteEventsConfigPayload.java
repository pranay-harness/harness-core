package software.wings.graphql.schema.mutation.event.payload;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDeleteEventsConfigPayload implements QLMutationPayload {
  String clientMutationId;
}
