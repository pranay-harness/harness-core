package io.harness.ngtriggers.service;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

public interface TriggerWebhookService {
  void registerIterators();
  void handle(TriggerWebhookEvent event);
}
