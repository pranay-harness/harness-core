package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.CUSTOM;

import io.harness.ngtriggers.beans.entity.metadata.AuthToken;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomWebhookTriggerSpec implements WebhookTriggerSpec {
  List<WebhookCondition> payloadConditions;
  List<WebhookCondition> headerConditions;
  AuthToken authToken;

  public void setRepoSpec(RepoSpec repoUrl) {}
  public void setEvent(WebhookEvent webhookEvent) {}

  public void setActions(List<WebhookAction> webhookActions) {}

  public void setPathFilters(List<String> pathFilters) {}

  @Override
  public RepoSpec getRepoSpec() {
    return null;
  }

  @Override
  public WebhookEvent getEvent() {
    return null;
  }

  @Override
  public List<WebhookAction> getActions() {
    return null;
  }

  @Override
  public List<String> getPathFilters() {
    return null;
  }

  @Override
  public WebhookSourceRepo getType() {
    return CUSTOM;
  }
}
