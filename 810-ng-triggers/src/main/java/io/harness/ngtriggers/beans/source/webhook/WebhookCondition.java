package io.harness.ngtriggers.beans.source.webhook;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookCondition {
  String key;
  String operator;
  String value;
}
