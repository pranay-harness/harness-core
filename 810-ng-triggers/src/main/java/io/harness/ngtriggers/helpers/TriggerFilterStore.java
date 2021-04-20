package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.CUSTOM;
import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.GITHUB;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.EventActionTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GitWebhookTriggerRepoFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GithubIssueCommentTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.JexlConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.PayloadConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.ProjectTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.SourceRepoTypeTriggerFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class TriggerFilterStore {
  private final GitWebhookTriggerRepoFilter gitWebhookTriggerRepoFilter;
  private final ProjectTriggerFilter projectTriggerFilter;
  private final SourceRepoTypeTriggerFilter sourceRepoTypeTriggerFilter;
  private final EventActionTriggerFilter eventActionTriggerFilter;
  private final PayloadConditionsTriggerFilter payloadConditionsTriggerFilter;
  private final GithubIssueCommentTriggerFilter githubIssueCommentTriggerFilter;
  private final JexlConditionsTriggerFilter jexlConditionsTriggerFilter;

  public List<TriggerFilter> getWebhookTriggerFilters(WebhookPayloadData webhookPayloadData) {
    if (CUSTOM.name().equals(webhookPayloadData.getOriginalEvent().getSourceRepoType())) {
      return Arrays.asList(projectTriggerFilter, payloadConditionsTriggerFilter, jexlConditionsTriggerFilter);
    }

    // When it github and comment on a pr event
    // webhookPayloadData.getParseWebhookResponse().getComment().getIssue().getPr() will be null,
    // when its comment on the issue
    if (webhookPayloadData.getParseWebhookResponse().hasComment()
        && webhookPayloadData.getParseWebhookResponse().getComment().getIssue() != null
        && webhookPayloadData.getParseWebhookResponse().getComment().getIssue().getPr() != null
        && GITHUB.name().equals(webhookPayloadData.getOriginalEvent().getSourceRepoType())) {
      return getTriggerFiltersGithubIssueCommentList();
    }

    return getWebhookGitTriggerFiltersDefaultList();
  }

  List<TriggerFilter> getWebhookGitTriggerFiltersDefaultList() {
    return Arrays.asList(projectTriggerFilter, sourceRepoTypeTriggerFilter, eventActionTriggerFilter,
        payloadConditionsTriggerFilter, gitWebhookTriggerRepoFilter, jexlConditionsTriggerFilter);
  }

  List<TriggerFilter> getTriggerFiltersGithubIssueCommentList() {
    return Arrays.asList(projectTriggerFilter, sourceRepoTypeTriggerFilter, eventActionTriggerFilter,
        gitWebhookTriggerRepoFilter, githubIssueCommentTriggerFilter);
  }
}
