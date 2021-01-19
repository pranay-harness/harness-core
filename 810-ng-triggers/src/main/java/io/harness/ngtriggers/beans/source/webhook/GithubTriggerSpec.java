package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.GITHUB;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GithubTriggerSpec implements WebhookTriggerSpec {
  String repoUrl;
  WebhookEvent event;
  List<WebhookAction> actions;
  List<WebhookPayloadCondition> payloadConditions;
  List<String> pathFilters;

  @Override
  public WebhookSourceRepo getType() {
    return GITHUB;
  }
}
