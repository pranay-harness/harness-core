package io.harness.ngtriggers.beans.scm;

import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParsePayloadResponse {
  private WebhookPayloadData webhookPayloadData;
  private TriggerWebhookEvent originalEvent;
  private boolean exceptionOccured;
  private Exception exception;
}
