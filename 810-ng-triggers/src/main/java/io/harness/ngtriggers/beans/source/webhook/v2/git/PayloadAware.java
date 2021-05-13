package io.harness.ngtriggers.beans.source.webhook.v2.git;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;

import java.util.List;

@OwnedBy(PIPELINE)
public interface PayloadAware {
  List<WebhookCondition> fetchHeaderConditions();
  List<WebhookCondition> fetchPayloadConditions();
  String fetchJexlCondition();
}
