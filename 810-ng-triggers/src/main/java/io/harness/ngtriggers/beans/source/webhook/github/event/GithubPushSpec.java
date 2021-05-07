package io.harness.ngtriggers.beans.source.webhook.github.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;

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
public class GithubPushSpec implements GithubEventSpec {
  String connectorRef;
  String repoName;
  List<WebhookCondition> headerConditions;
  List<WebhookCondition> payloadConditions;
  String jexlCondition;
  boolean autoAbortPreviousExecutions;
}
