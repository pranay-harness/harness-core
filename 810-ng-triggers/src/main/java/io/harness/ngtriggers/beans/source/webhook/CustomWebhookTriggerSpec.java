package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.CUSTOM;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.ngtriggers.beans.entity.metadata.AuthToken;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomWebhookTriggerSpec implements WebhookTriggerSpec {
  List<WebhookPayloadCondition> payloadConditions;
  AuthToken authToken;

  public void setRepoUrl(String repoUrl) {}
  public void setEvent(WebhookEvent webhookEvent) {}

  public void setActions(List<WebhookAction> webhookActions) {}

  public void setPathFilters(List<String> pathFilters) {}

  @Override
  public String getRepoUrl() {
    return EMPTY;
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
