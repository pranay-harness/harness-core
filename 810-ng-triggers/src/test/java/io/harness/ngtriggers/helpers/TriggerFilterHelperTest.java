package io.harness.ngtriggers.helpers;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventBuilder;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData.WebhookPayloadDataBuilder;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.EventActionTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GitWebhookTriggerRepoFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.GithubIssueCommentTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.PayloadConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.ProjectTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.SourceRepoTypeTriggerFilter;
import io.harness.product.ci.scm.proto.Issue;
import io.harness.product.ci.scm.proto.IssueCommentHook;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TriggerFilterHelperTest extends CategoryTest {
  @Mock GitWebhookTriggerRepoFilter gitWebhookTriggerRepoFilter;
  @Mock ProjectTriggerFilter projectTriggerFilter;
  @Mock SourceRepoTypeTriggerFilter sourceRepoTypeTriggerFilter;
  @Mock EventActionTriggerFilter eventActionTriggerFilter;
  @Mock PayloadConditionsTriggerFilter payloadConditionsTriggerFilter;
  @Mock GithubIssueCommentTriggerFilter githubIssueCommentTriggerFilter;
  @Inject @InjectMocks TriggerFilterStore triggerFilterStore;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetWebhookTriggerFilters() {
    TriggerWebhookEventBuilder originalEventBuilder = TriggerWebhookEvent.builder().sourceRepoType("CUSTOM");
    WebhookPayloadDataBuilder webhookPayloadDataBuilder = WebhookPayloadData.builder();

    List<TriggerFilter> webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.originalEvent(originalEventBuilder.build()).build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(projectTriggerFilter, payloadConditionsTriggerFilter);

    TriggerFilter[] triggerFiltersDefaultGit = new TriggerFilter[] {projectTriggerFilter, sourceRepoTypeTriggerFilter,
        eventActionTriggerFilter, payloadConditionsTriggerFilter, gitWebhookTriggerRepoFilter};

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITLAB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("BITBUCKET").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder.parseWebhookResponse(ParseWebhookResponse.newBuilder().build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITHUB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters).containsExactlyInAnyOrder(triggerFiltersDefaultGit);

    webhookTriggerFilters = triggerFilterStore.getWebhookTriggerFilters(
        webhookPayloadDataBuilder
            .parseWebhookResponse(
                ParseWebhookResponse.newBuilder()
                    .setComment(
                        IssueCommentHook.newBuilder()
                            .setIssue(Issue.newBuilder().setPr(PullRequest.newBuilder().setNumber(1).build()).build())
                            .build())
                    .build())
            .originalEvent(originalEventBuilder.sourceRepoType("GITHUB").build())
            .build());
    assertThat(webhookTriggerFilters).isNotNull();
    assertThat(webhookTriggerFilters)
        .containsExactlyInAnyOrder(projectTriggerFilter, sourceRepoTypeTriggerFilter, eventActionTriggerFilter,
            gitWebhookTriggerRepoFilter, githubIssueCommentTriggerFilter);
  }
}
