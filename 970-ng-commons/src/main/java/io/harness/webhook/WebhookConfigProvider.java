package io.harness.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface WebhookConfigProvider {
  String getWebhookApiBaseUrl();
  String getCustomApiBaseUrl();
}
