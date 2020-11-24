package io.harness.ngtriggers.beans.scm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ParsePayloadResponse {
  private WebhookPayloadData webhookPayloadData;
  private boolean exceptionOccured;
  private Exception exception;
}
