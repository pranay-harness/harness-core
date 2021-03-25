package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookEventType.PUSH;

import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse.ParsePayloadResponseBuilder;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.WebhookEventToTriggerMapper;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.TriggerFilterStore;
import io.harness.ngtriggers.helpers.WebhookEventPublisher;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitWebhookEventToTriggerMapper implements WebhookEventToTriggerMapper {
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  private final TriggerFilterStore triggerFilterHelper;
  private final WebhookEventPublisher webhookEventPublisher;

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    String projectFqn = getProjectFqn(triggerWebhookEvent);

    // 1. Parse Payload
    ParsePayloadResponse parsePayloadResponse = convertWebhookResponse(triggerWebhookEvent);
    if (parsePayloadResponse.isExceptionOccured()) {
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.prepareResponseForScmException(parsePayloadResponse))
          .build();
    }

    WebhookPayloadData webhookPayloadData = parsePayloadResponse.getWebhookPayloadData();

    publishPushEvent(webhookPayloadData);

    // Generate list of all filters to be applied
    FilterRequestData filterRequestData =
        FilterRequestData.builder().projectFqn(projectFqn).webhookPayloadData(webhookPayloadData).build();
    List<TriggerFilter> triggerFilters =
        triggerFilterHelper.getWebhookTriggerFilters(filterRequestData.getWebhookPayloadData());

    // Apply filters
    WebhookEventMappingResponse webhookEventMappingResponse = null;
    TriggerFilter triggerFilterInAction = null;

    try {
      for (TriggerFilter triggerFilter : triggerFilters) {
        triggerFilterInAction = triggerFilter;
        webhookEventMappingResponse = triggerFilter.applyFilter(filterRequestData);
        if (webhookEventMappingResponse.isFailedToFindTrigger()) {
          return webhookEventMappingResponse;
        } else {
          // update with updated filter list for next filter
          filterRequestData.setDetails(webhookEventMappingResponse.getTriggers());
        }
      }
    } catch (Exception e) {
      return triggerFilterInAction.getWebhookResponseForException(filterRequestData, e);
    }

    return webhookEventMappingResponse;
  }

  /**
   * This is temporary, added specifically to support TI use-case.
   * We only publish "PUSH" git event.
   * This will become part of common service, where different subscribers can subscribe for
   * eventType, triggerType to receive events.
   * <p>
   * Then this can be removed.
   *
   * @param webhookPayloadData
   */
  @VisibleForTesting
  void publishPushEvent(WebhookPayloadData webhookPayloadData) {
    try {
      if (webhookPayloadData.getParseWebhookResponse().hasPush()) {
        webhookEventPublisher.publishGitWebhookEvent(webhookPayloadData, PUSH);
      }
    } catch (Exception e) {
      log.error("Failed to send webhook event {} to events framework: {}",
          webhookPayloadData.getOriginalEvent().getUuid(), e);
    }
  }

  private String getProjectFqn(TriggerWebhookEvent triggerWebhookEvent) {
    return new StringBuilder(256)
        .append(triggerWebhookEvent.getAccountId())
        .append('/')
        .append(triggerWebhookEvent.getOrgIdentifier())
        .append('/')
        .append(triggerWebhookEvent.getProjectIdentifier())
        .toString();
  }

  // Add error handling
  @VisibleForTesting
  ParsePayloadResponse convertWebhookResponse(TriggerWebhookEvent triggerWebhookEvent) {
    ParsePayloadResponseBuilder builder = ParsePayloadResponse.builder();
    try {
      WebhookPayloadData webhookPayloadData = webhookEventPayloadParser.parseEvent(triggerWebhookEvent);
      builder.webhookPayloadData(webhookPayloadData).build();
    } catch (Exception e) {
      builder.exceptionOccured(true)
          .exception(e)
          .webhookPayloadData(WebhookPayloadData.builder().originalEvent(triggerWebhookEvent).build())
          .build();
    }

    return builder.build();
  }
}
