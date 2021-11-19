package io.harness.ng.webhook.services.api;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.webhook.entities.WebhookEvent;

@OwnedBy(PIPELINE)
public interface WebhookEventProcessingService {
  void registerIterators(int threadPoolSize);
  void handle(WebhookEvent event);
}
