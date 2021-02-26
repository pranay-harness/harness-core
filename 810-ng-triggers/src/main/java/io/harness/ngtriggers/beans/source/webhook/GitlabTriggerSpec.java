package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.GITLAB;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitlabTriggerSpec implements WebhookTriggerSpec {
  GitRepoSpec gitRepoSpec;
  WebhookEvent event;
  List<WebhookAction> actions;
  List<WebhookCondition> payloadConditions;
  List<String> pathFilters;

  @Override
  public WebhookSourceRepo getType() {
    return GITLAB;
  }

  @Override
  public RepoSpec getRepoSpec() {
    return gitRepoSpec;
  }
}
