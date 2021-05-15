package io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent.PUSH;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class GitlabPushSpec implements GitlabEventSpec {
  String connectorRef;
  String repoName;
  List<WebhookCondition> headerConditions;
  List<WebhookCondition> payloadConditions;
  String jexlCondition;
  boolean autoAbortPreviousExecutions;

  @Override
  public String fetchConnectorRef() {
    return connectorRef;
  }

  @Override
  public String fetchRepoName() {
    return repoName;
  }

  @Override
  public GitEvent fetchEvent() {
    return PUSH;
  }

  @Override
  public List<GitAction> fetchActions() {
    return emptyList();
  }

  @Override
  public List<WebhookCondition> fetchHeaderConditions() {
    return headerConditions;
  }

  @Override
  public List<WebhookCondition> fetchPayloadConditions() {
    return payloadConditions;
  }

  @Override
  public String fetchJexlCondition() {
    return jexlCondition;
  }

  @Override
  public boolean fetchAutoAbortPreviousExecutions() {
    return autoAbortPreviousExecutions;
  }
}
