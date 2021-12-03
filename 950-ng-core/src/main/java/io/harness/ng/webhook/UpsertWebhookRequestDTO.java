package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "UpsertWebhookRequest",
    description = "This is the view of the UpsertWebhookRequest entity defined in Harness")
public class UpsertWebhookRequestDTO {
  @NotNull @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull @NotEmpty String connectorIdentifierRef;
  @NotNull HookEventType hookEventType;
  String repoURL;
}
