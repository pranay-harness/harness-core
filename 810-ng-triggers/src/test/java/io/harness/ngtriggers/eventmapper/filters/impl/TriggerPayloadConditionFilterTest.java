package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS;
import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PRWebhookEvent;
import io.harness.beans.WebhookBaseAttributes;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.NgTriggersTestHelper;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.webhook.AwsCodeCommitTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GithubTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class TriggerPayloadConditionFilterTest extends CategoryTest {
  @Mock private NGTriggerService ngTriggerService;
  @Mock private NGTriggerElementMapper ngTriggerElementMapper;
  @Inject @InjectMocks private PayloadConditionsTriggerFilter filter;
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
            .webhookEvent(PRWebhookEvent.builder()
                              .baseAttributes(WebhookBaseAttributes.builder().source("test").target("main").build())
                              .build())
            .originalEvent(TriggerWebhookEvent.builder().accountId("acc").createdAt(0l).build())
            .parseWebhookResponse(
                ParseWebhookResponse.newBuilder()
                    .setPr(PullRequestHook.newBuilder().setPr(PullRequest.newBuilder().setNumber(1).build()).build())
                    .build())
            .build();

    NGTriggerEntity triggerEntityGithub1 =
        NGTriggerEntity.builder()
            .metadata(NGTriggerMetadata.builder().webhook(WebhookMetadata.builder().type("GITHUB").build()).build())
            .yaml("yaml")
            .enabled(true)
            .build();

    NGTriggerConfig ngTriggerConfig1 =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder()
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(Arrays.asList(WebhookCondition.builder()
                                                                                 .key("sourceBranch")
                                                                                 .operator("equals")
                                                                                 .value("test")
                                                                                 .build(),
                                                WebhookCondition.builder()
                                                    .key("targetBranch")
                                                    .operator("equals")
                                                    .value("master")
                                                    .build()))
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfig ngTriggerConfig2 =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder()
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(Arrays.asList(WebhookCondition.builder()
                                                                                 .key("sourceBranch")
                                                                                 .operator("in")
                                                                                 .value("test,uat")
                                                                                 .build(),
                                                WebhookCondition.builder()
                                                    .key("targetBranch")
                                                    .operator("equals")
                                                    .value("main")
                                                    .build()))
                                            .build())
                                  .build())
                        .build())
            .build();

    // No payload match
    doReturn(ngTriggerConfig1)
        .doReturn(ngTriggerConfig1)
        .doReturn(ngTriggerConfig2)
        .when(ngTriggerElementMapper)
        .toTriggerConfig("yaml");

    // Case 1
    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(
        FilterRequestData.builder()
            .details(Arrays.asList(TriggerDetails.builder().ngTriggerEntity(triggerEntityGithub1).build()))
            .webhookPayloadData(webhookPayloadData)
            .accountId("p")
            .build());

    assertThat(webhookEventMappingResponse.getWebhookEventResponse()).isNotNull();
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getFinalStatus())
        .isEqualTo(NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS);

    // Trigger found
    webhookEventMappingResponse = filter.applyFilter(
        FilterRequestData.builder()
            .details(Arrays.asList(TriggerDetails.builder().ngTriggerEntity(triggerEntityGithub1).build(),
                TriggerDetails.builder().ngTriggerEntity(triggerEntityGithub1).build()))
            .webhookPayloadData(webhookPayloadData)
            .accountId("p")
            .build());

    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    assertThat(webhookEventMappingResponse.getTriggers().size()).isEqualTo(1);
    assertThat(webhookEventMappingResponse.getTriggers().get(0).getNgTriggerConfig()).isEqualTo(ngTriggerConfig2);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFilterAwsCodeCommitPayload() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder()
            .webhookEvent(NgTriggersTestHelper.getAwsCodecommitPushWebhookEvent())
            .originalEvent(NgTriggersTestHelper.getTriggerWehookEventForAwsCodeCommitPush())
            .parseWebhookResponse(ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().build()).build())
            .build();

    NGTriggerEntity triggerEntityAwsCodeCommit =
        NGTriggerEntity.builder()
            .metadata(
                NGTriggerMetadata.builder().webhook(WebhookMetadata.builder().type("AWS_CODECOMMIT").build()).build())
            .yaml("yaml")
            .enabled(true)
            .build();

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("AWS_CODECOMMIT")
                                  .spec(AwsCodeCommitTriggerSpec.builder()
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(Arrays.asList(WebhookCondition.builder()
                                                                                 .key("targetBranch")
                                                                                 .operator("equals")
                                                                                 .value("main")
                                                                                 .build(),
                                                WebhookCondition.builder()
                                                    .key("<+trigger.payload.Type>")
                                                    .operator("equals")
                                                    .value("Notification")
                                                    .build()))
                                            .build())
                                  .build())
                        .build())
            .build();

    // No payload match
    doReturn(ngTriggerConfig).doReturn(ngTriggerConfig).when(ngTriggerElementMapper).toTriggerConfig("yaml");

    // Case 1
    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(
        FilterRequestData.builder()
            .details(
                Collections.singletonList(TriggerDetails.builder().ngTriggerEntity(triggerEntityAwsCodeCommit).build()))
            .webhookPayloadData(webhookPayloadData)
            .accountId("p")
            .build());

    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    assertThat(webhookEventMappingResponse.getTriggers().size()).isEqualTo(1);
    assertThat(webhookEventMappingResponse.getTriggers().get(0).getNgTriggerConfig()).isEqualTo(ngTriggerConfig);
  }
}
