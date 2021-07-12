package io.harness.app.schema.mutation.delegate.input;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
@Scope(PermissionAttribute.ResourceType.APPLICATION)

public class QLAttachScopeToDelegateInput implements QLMutationInput {
  String clientMutationId;

  String accountId;
  String delegateId;
  List<String> includeScopes;
  List<String> excludeScopes;
}
