package io.harness.ng.webhook.services.api;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ng.webhook.entities.WebhookEvent;

@OwnedBy(PIPELINE)
public interface WebhookService {
  WebhookEvent addEventToQueue(WebhookEvent webhookEvent);
  UpsertWebhookResponseDTO upsertWebhook(UpsertWebhookRequestDTO upsertWebhookRequestDTO);
}
