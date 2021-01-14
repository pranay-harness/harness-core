package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOUND_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_CONDITIONS;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.scm.Repository;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WebhookEventResponseHelper.class})
public class WebhookEventToTriggerMapTest extends CategoryTest {
  @Mock WebhookEventPayloadParser webhookEventPayloadParser;
  @Mock NGTriggerElementMapper ngTriggerElementMapper;
  @Mock NGTriggerService ngTriggerService;
  @InjectMocks @Inject GitWebhookEventToTriggerMapper gitWebhookEventToTriggerMapper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testMapWebhookEventToTriggers() {
    GitWebhookEventToTriggerMapper mapper =
        new GitWebhookEventToTriggerMapper(ngTriggerService, webhookEventPayloadParser, ngTriggerElementMapper);
    GitWebhookEventToTriggerMapper spyMapper = spy(mapper);

    TriggerWebhookEvent event = TriggerWebhookEvent.builder().build();
    ParsePayloadResponse response = ParsePayloadResponse.builder().exceptionOccured(true).build();
    WebhookEventResponse eventResponse;

    // parsing failed
    mockStatic(WebhookEventResponseHelper.class);
    eventResponse = WebhookEventResponse.builder().message("parsing failed").build();
    when(WebhookEventResponseHelper.prepareResponseForScmException(response)).thenReturn(eventResponse);
    doReturn(response).when(spyMapper).convertWebhookResponse(event);
    WebhookEventMappingResponse webhookEventMappingResponse = spyMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse.getWebhookEventResponse()).isEqualTo(eventResponse);
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getMessage()).isEqualTo("parsing failed");

    // No trigger found for repo
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().repository(Repository.builder().link("abc.com").build()).build();
    response = ParsePayloadResponse.builder().webhookPayloadData(webhookPayloadData).build();
    doReturn(response).when(spyMapper).convertWebhookResponse(event);

    doReturn(null).when(spyMapper).retrieveTriggersConfiguredForRepo(
        event, Repository.builder().link("abc.com").build());
    eventResponse = WebhookEventResponse.builder().message("No Trigger was configured for Repo: abc.com").build();
    when(WebhookEventResponseHelper.toResponse(
             NO_MATCHING_TRIGGER_FOR_REPO, event, null, null, "No Trigger was configured for Repo: abc.com", null))
        .thenReturn(eventResponse);
    webhookEventMappingResponse = spyMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getTriggers()).isEmpty();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getMessage())
        .isEqualTo("No Trigger was configured for Repo: abc.com");

    // No enabled trigger found for repo
    doReturn(Arrays.asList(NGTriggerEntity.builder().uuid("abc").identifier("i").enabled(false).build()))
        .when(spyMapper)
        .retrieveTriggersConfiguredForRepo(event, Repository.builder().link("abc.com").build());
    eventResponse =
        WebhookEventResponse.builder().message("No Trigger configured for Repo was in ENABLED status: abc.com").build();
    when(WebhookEventResponseHelper.toResponse(NO_ENABLED_TRIGGER_FOUND_FOR_REPO, event, null, null,
             "No Trigger configured for Repo was in ENABLED status: abc.com", null))
        .thenReturn(eventResponse);
    webhookEventMappingResponse = spyMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getTriggers()).isEmpty();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getMessage())
        .isEqualTo("No Trigger configured for Repo was in ENABLED status: abc.com");

    // No trigger matched conditions for repo
    List<NGTriggerEntity> ngTriggerEntities =
        Arrays.asList(NGTriggerEntity.builder().uuid("abc").identifier("i").enabled(true).build());
    doReturn(ngTriggerEntities)
        .when(spyMapper)
        .retrieveTriggersConfiguredForRepo(event, Repository.builder().link("abc.com").build());
    doReturn(null).when(spyMapper).applyFilters(webhookPayloadData, ngTriggerEntities);
    eventResponse = WebhookEventResponse.builder().message("No Trigger matched conditions for payload event").build();
    when(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_CONDITIONS, event, null, null,
             "No Trigger matched conditions for payload event", null))
        .thenReturn(eventResponse);
    webhookEventMappingResponse = spyMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getMessage())
        .isEqualTo("No Trigger matched conditions for payload event");

    // Trigger found
    doReturn(ngTriggerEntities).when(spyMapper).applyFilters(webhookPayloadData, ngTriggerEntities);
    webhookEventMappingResponse = spyMapper.mapWebhookEventToTriggers(event);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    assertThat(webhookEventMappingResponse.getTriggers()).isEqualTo(ngTriggerEntities);
  }
}
