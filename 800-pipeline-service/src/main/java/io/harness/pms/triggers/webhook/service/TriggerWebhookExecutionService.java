package io.harness.pms.triggers.webhook.service;

import io.harness.mongo.iterator.IteratorConfig;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

public interface TriggerWebhookExecutionService {
  void registerIterators(IteratorConfig iteratorConfig);
  void handle(TriggerWebhookEvent event);
}
