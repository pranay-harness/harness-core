package io.harness.ngtriggers.beans.source.webhook;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WebhookTriggerSpec {
  WebhookEvent event;
  List<WebhookAction> actions;
  List<WebhookPayloadCondition> payloadConditions;
  List<String> pathFilters;
}
