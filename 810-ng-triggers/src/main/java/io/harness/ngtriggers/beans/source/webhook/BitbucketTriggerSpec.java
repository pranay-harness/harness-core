package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.BITBUCKET;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BitbucketTriggerSpec implements WebhookTriggerSpec {
  String repoUrl;
  WebhookEvent event;
  List<WebhookAction> actions;
  List<WebhookCondition> payloadConditions;
  List<String> pathFilters;

  @Override
  public WebhookSourceRepo getType() {
    return BITBUCKET;
  }
}
