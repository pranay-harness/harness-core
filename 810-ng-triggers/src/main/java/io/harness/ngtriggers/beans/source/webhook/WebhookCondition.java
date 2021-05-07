package io.harness.ngtriggers.beans.source.webhook;

import io.harness.ngtriggers.conditionchecker.ConditionOperator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookCondition {
  String key;
  ConditionOperator operator;
  String value;
}
