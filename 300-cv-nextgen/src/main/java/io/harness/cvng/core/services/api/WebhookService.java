package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.PagerDutyWebhookEvent;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.PagerDutyWebhook;
import io.harness.cvng.core.entities.Webhook;

public interface WebhookService extends DeleteEntityByHandler<Webhook> {
  void createPagerdutyWebhook(
      ServiceEnvironmentParams serviceEnvironmentParams, String token, String webhookId, String changeSourceId);

  PagerDutyWebhook getPagerdutyWebhook(ProjectParams projectParams, String changeSourceId);

  void deleteWebhook(Webhook webhook);

  void handlePagerDutyWebhook(String token, PagerDutyWebhookEvent payload);
}
