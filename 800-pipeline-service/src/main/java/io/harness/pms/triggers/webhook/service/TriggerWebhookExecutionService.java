package io.harness.pms.triggers.webhook.service;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

public interface TriggerWebhookExecutionService {
  void registerIterators();
  void handle(TriggerWebhookEvent event);
}
