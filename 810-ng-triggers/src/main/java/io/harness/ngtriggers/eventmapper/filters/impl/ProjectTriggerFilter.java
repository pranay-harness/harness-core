package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOR_PROJECT;

import static java.util.stream.Collectors.toList;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.service.NGTriggerService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class ProjectTriggerFilter implements TriggerFilter {
  private final NGTriggerService ngTriggerService;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder builder = WebhookEventMappingResponse.builder();
    TriggerWebhookEvent triggerWebhookEvent = filterRequestData.getWebhookPayloadData().getOriginalEvent();
    List<NGTriggerEntity> triggersForCurrentProject =
        ngTriggerService.listEnabledTriggersForCurrentProject(triggerWebhookEvent.getAccountId(),
            triggerWebhookEvent.getOrgIdentifier(), triggerWebhookEvent.getProjectIdentifier());

    if (hasNone(triggersForCurrentProject)) {
      String errorMsg = "No enabled trigger found for project:" + filterRequestData.getProjectFqn();
      log.info(errorMsg);
      builder.failedToFindTrigger(true).webhookEventResponse(WebhookEventResponseHelper.toResponse(
          NO_ENABLED_TRIGGER_FOR_PROJECT, triggerWebhookEvent, null, null, errorMsg, null));
    } else {
      addDetails(builder, filterRequestData,
          triggersForCurrentProject.stream()
              .map(entity -> TriggerDetails.builder().ngTriggerEntity(entity).build())
              .collect(toList()));
    }

    return builder.build();
  }
}
