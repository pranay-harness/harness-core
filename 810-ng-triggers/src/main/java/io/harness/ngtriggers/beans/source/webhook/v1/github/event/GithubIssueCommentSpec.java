package io.harness.ngtriggers.beans.source.webhook.v1.github.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.v1.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v1.git.PayloadAware;
import io.harness.ngtriggers.beans.source.webhook.v1.github.action.GithubIssueCommentAction;

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
public class GithubIssueCommentSpec implements GithubEventSpec, PayloadAware, GitAware {
  String connectorRef;
  String repoName;
  List<GithubIssueCommentAction> actions;
  List<WebhookCondition> headerConditions;
  List<WebhookCondition> payloadConditions;
  String jexlCondition;
  boolean autoAbortPreviousExecutions;
}
