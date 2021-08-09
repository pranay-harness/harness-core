package io.harness.ngtriggers.beans.source.webhook.v2.git;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import java.util.List;

@OwnedBy(PIPELINE)
public interface PayloadAware {
  List<TriggerEventDataCondition> fetchHeaderConditions();
  List<TriggerEventDataCondition> fetchPayloadConditions();
  String fetchJexlCondition();
}
