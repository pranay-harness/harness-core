package io.harness.ng.webhook.services.api;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;
import io.harness.delegate.task.scm.ScmGitWebhookTaskResponseData;
import io.harness.ng.webhook.entities.WebhookEvent;

@OwnedBy(PIPELINE)
public interface WebhookService {
  WebhookEvent addEventToQueue(WebhookEvent webhookEvent);
  ScmGitWebhookTaskResponseData upsertWebhook(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierRef, String target, HookEventType hookEventType, String repoURL);
}
