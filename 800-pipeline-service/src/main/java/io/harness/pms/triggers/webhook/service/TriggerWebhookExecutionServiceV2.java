package io.harness.pms.triggers.webhook.service;

import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;

public interface TriggerWebhookExecutionServiceV2 {
  void processEvent(WebhookDTO webhookDTO);
}
