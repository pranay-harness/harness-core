package io.harness.ngtriggers.beans.source.webhook.v2;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class TriggerEventDataCondition {
  String key;
  ConditionOperator operator;
  String value;
}
