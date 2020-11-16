package io.harness.ng.ngtriggers.intfc;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public interface TriggerWebhookService {
  void registerIterators(ScheduledThreadPoolExecutor webhookEventExecutor);
  void handle(TriggerWebhookEvent event);
}
