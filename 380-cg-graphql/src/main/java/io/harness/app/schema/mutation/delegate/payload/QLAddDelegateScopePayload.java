package io.harness.app.schema.mutation.delegate.payload;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.type.delegate.QLDelegateScope;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLAddDelegateScopePayload implements QLMutationPayload {
  String clientMutationId;
  String message;
  QLDelegateScope delegateScope;
}
