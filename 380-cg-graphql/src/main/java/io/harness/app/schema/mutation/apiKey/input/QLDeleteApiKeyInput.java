package io.harness.app.schema.mutation.apiKey.input;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteApiKeyInput implements QLMutationInput {
  String clientMutationId;
  String uuid;
  String accountId;
}
