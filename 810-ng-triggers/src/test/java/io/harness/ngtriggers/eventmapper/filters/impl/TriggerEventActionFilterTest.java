package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_EVENT_ACTION;
import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.PULL_REQUEST;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.beans.PRWebhookEvent;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GithubTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TriggerEventActionFilterTest extends CategoryTest {
  @Mock private NGTriggerService ngTriggerService;
  @Mock private NGTriggerElementMapper ngTriggerElementMapper;
  @Inject @InjectMocks private EventActionTriggerFilter filter;
  private static List<NGTriggerEntity> triggerEntities;

  @Before
  public void setUp() throws IOException {
    initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void applyFilterTest() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder()
            .webhookEvent(PRWebhookEvent.builder().build())
            .originalEvent(TriggerWebhookEvent.builder().accountId("acc").createdAt(0l).build())
            .parseWebhookResponse(
                ParseWebhookResponse.newBuilder()
                    .setPr(PullRequestHook.newBuilder().setPr(PullRequest.newBuilder().setNumber(1).build()).build())
                    .build())
            .build();

    NGTriggerEntity triggerEntityGithub =
        NGTriggerEntity.builder()
            .metadata(NGTriggerMetadata.builder().webhook(WebhookMetadata.builder().type("GITHUB").build()).build())
            .yaml("yaml")
            .enabled(true)
            .build();

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder().event(WebhookEvent.PUSH).build())
                                  .build())
                        .build())
            .build();

    // No payload match
    doReturn(ngTriggerConfig).when(ngTriggerElementMapper).toTriggerConfig("yaml");
    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(
        FilterRequestData.builder()
            .details(Arrays.asList(TriggerDetails.builder().ngTriggerEntity(triggerEntityGithub).build()))
            .webhookPayloadData(webhookPayloadData)
            .projectFqn("p")
            .build());

    assertThat(webhookEventMappingResponse.getWebhookEventResponse()).isNotNull();
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getFinalStatus())
        .isEqualTo(NO_MATCHING_TRIGGER_FOR_EVENT_ACTION);

    // Trigger found
    NGTriggerSpec spec = ngTriggerConfig.getSource().getSpec();
    ((GithubTriggerSpec) ((WebhookTriggerConfig) spec).getSpec()).setEvent(PULL_REQUEST);

    webhookEventMappingResponse = filter.applyFilter(
        FilterRequestData.builder()
            .details(Arrays.asList(TriggerDetails.builder().ngTriggerEntity(triggerEntityGithub).build()))
            .projectFqn("p")
            .webhookPayloadData(webhookPayloadData)
            .build());

    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    assertThat(webhookEventMappingResponse.getTriggers().size()).isEqualTo(1);
    assertThat(webhookEventMappingResponse.getTriggers().get(0).getNgTriggerEntity()).isEqualTo(triggerEntityGithub);
  }
}
