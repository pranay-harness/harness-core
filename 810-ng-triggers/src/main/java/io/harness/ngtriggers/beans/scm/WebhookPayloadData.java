package io.harness.ngtriggers.beans.scm;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookPayloadData {
  private WebhookGitUser webhookGitUser;
  private Repository repository;
  private WebhookEvent webhookEvent;
  private TriggerWebhookEvent originalEvent;
  private ParseWebhookResponse parseWebhookResponse;
}
