package io.harness.ngtriggers.beans.source.webhook.v2;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.artifact.EventCondition;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;

@OwnedBy(PIPELINE)
public interface TriggerEventCondition {
  EventCondition getEventCondition();
  ConditionOperator getConditionOperator();
  String getValue();
}
