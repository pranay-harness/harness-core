package io.harness.app.schema.mutation.delegate.payload;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@OwnedBy(DEL)
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDeleteDelegatePayload implements QLMutationPayload {
  String clientMutationId;
  String message;
}
