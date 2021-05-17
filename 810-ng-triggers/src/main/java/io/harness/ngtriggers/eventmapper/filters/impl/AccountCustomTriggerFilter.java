package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_ACCOUNT;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(PIPELINE)
public class AccountCustomTriggerFilter implements TriggerFilter {
  private final NGTriggerService ngTriggerService;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder builder = initWebhookEventMappingResponse(filterRequestData);
    TriggerWebhookEvent triggerWebhookEvent = filterRequestData.getWebhookPayloadData().getOriginalEvent();
    List<NGTriggerEntity> triggersForAccount =
        ngTriggerService.findTriggersForCustomWehbook(triggerWebhookEvent, false, true);

    if (isEmpty(triggersForAccount)) {
      String errorMsg = "No enabled custom trigger found for Account:" + triggerWebhookEvent.getAccountId();
      log.info(errorMsg);
      builder.failedToFindTrigger(true).webhookEventResponse(WebhookEventResponseHelper.toResponse(
          NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_ACCOUNT, triggerWebhookEvent, null, null, errorMsg, null));
    } else {
      addDetails(builder, filterRequestData,
          triggersForAccount.stream()
              .map(entity -> TriggerDetails.builder().ngTriggerEntity(entity).build())
              .collect(toList()));
    }

    return builder.build();
  }
}
