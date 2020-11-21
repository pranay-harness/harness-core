package io.harness.ngtriggers.beans.scm;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookPayloadData {
  private WebhookGitUser webhookGitUser;
  private Repository repository;
  private WebhookEvent webhookEvent;
  private TriggerWebhookEvent originalEvent;
}
