package io.harness.app.schema.mutation.template.input;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@Scope(PermissionAttribute.ResourceType.TEMPLATE)
public class QLTemplateListInput implements QLMutationInput {
  String clientMutationId;

  String accountId;
  String limit;
  String offset;
}
