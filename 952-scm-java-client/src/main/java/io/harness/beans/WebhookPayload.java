package io.harness.beans;

import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookPayload {
  private WebhookGitUser webhookGitUser;
  private Repository repository;
  private WebhookEvent webhookEvent;
  private ParseWebhookResponse parseWebhookResponse;
}
