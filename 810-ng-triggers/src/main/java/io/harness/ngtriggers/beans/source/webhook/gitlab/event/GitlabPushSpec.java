package io.harness.ngtriggers.beans.source.webhook.gitlab.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.github.event.GitlabEventSpec;

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
}
