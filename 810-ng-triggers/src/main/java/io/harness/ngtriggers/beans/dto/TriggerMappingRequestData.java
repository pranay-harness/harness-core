package io.harness.ngtriggers.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class TriggerMappingRequestData {
  TriggerWebhookEvent triggerWebhookEvent;
  WebhookDTO webhookDTO;
}
