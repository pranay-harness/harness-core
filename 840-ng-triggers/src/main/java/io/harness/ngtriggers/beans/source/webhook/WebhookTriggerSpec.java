package io.harness.ngtriggers.beans.source.webhook;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookTriggerSpec {
  String repoUrl;
  WebhookEvent event;
  List<WebhookAction> actions;
  List<WebhookPayloadCondition> payloadConditions;
  List<String> pathFilters;
}
