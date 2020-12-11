package io.harness.ngtriggers.beans.source.webhook;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookPayloadCondition {
  String key;
  String operator;
  String value;
}
