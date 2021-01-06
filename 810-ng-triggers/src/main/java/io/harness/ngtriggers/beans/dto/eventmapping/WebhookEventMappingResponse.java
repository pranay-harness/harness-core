package io.harness.ngtriggers.beans.dto.eventmapping;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookEventMappingResponse {
  WebhookEventResponse webhookEventResponse;
  ParseWebhookResponse parseWebhookResponse;
  @Default boolean failedToFindTrigger = true;
  @Default boolean isCustomTrigger = false;
  @Singular List<TriggerDetails> triggers;
}
