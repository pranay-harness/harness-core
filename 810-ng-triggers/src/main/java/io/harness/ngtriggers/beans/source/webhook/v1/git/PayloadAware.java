package io.harness.ngtriggers.beans.source.webhook.v1.git;

import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;

import java.util.List;

public interface PayloadAware {
  List<WebhookCondition> getHeaderConditions();
  List<WebhookCondition> getPayloadConditions();
  String getJexlCondition();
}
