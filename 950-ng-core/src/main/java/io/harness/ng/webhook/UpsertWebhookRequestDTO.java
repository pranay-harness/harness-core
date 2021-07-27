package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "UpsertWebhookRequestDTOKeys")
@OwnedBy(DX)
public class UpsertWebhookRequestDTO {
  @NotNull @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull @NotEmpty String connectorIdentifierRef;
  @NotNull HookEventType hookEventType;
  String repoURL;
}
