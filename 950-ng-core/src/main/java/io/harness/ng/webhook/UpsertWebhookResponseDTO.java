package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.product.ci.scm.proto.WebhookResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "UpsertWebhookResponseDTOKeys")
@OwnedBy(DX)
@Schema(name = "UpsertWebhookResponse",
    description = "This is the view of the UpsertWebhookResponse entity defined in Harness")
public class UpsertWebhookResponseDTO {
  WebhookResponse webhookResponse;
  int status;
  String error;
}
