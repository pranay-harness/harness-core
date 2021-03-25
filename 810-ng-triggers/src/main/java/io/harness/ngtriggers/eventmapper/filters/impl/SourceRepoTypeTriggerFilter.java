package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOR_SOURCEREPO_TYPE;

import static java.util.stream.Collectors.toList;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class SourceRepoTypeTriggerFilter implements TriggerFilter {
  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder builder = WebhookEventMappingResponse.builder();
    TriggerWebhookEvent triggerWebhookEvent = filterRequestData.getWebhookPayloadData().getOriginalEvent();
    List<TriggerDetails> filteredList = null;

    if (hasSome(filterRequestData.getDetails())) {
      filteredList = filterRequestData.getDetails()
                         .stream()
                         .filter(trigger -> isValidSourceRepoType(triggerWebhookEvent, trigger.getNgTriggerEntity()))
                         .collect(toList());
    }

    if (hasNone(filteredList)) {
      String msg = String.format("No enabled trigger found for sourceRepoType {} for project: {}",
          triggerWebhookEvent.getSourceRepoType(), filterRequestData.getProjectFqn());
      log.info(msg);
      builder.failedToFindTrigger(true)
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(
              NO_ENABLED_TRIGGER_FOR_SOURCEREPO_TYPE, triggerWebhookEvent, null, null, msg, null))
          .build();
    } else {
      addDetails(builder, filterRequestData, filteredList);
    }

    return builder.build();
  }

  @VisibleForTesting
  boolean isValidSourceRepoType(TriggerWebhookEvent triggerWebhookEvent, NGTriggerEntity triggerEntity) {
    if (triggerEntity.getMetadata().getWebhook() == null) {
      return false;
    }

    WebhookMetadata webhook = triggerEntity.getMetadata().getWebhook();
    return webhook.getType().equalsIgnoreCase(triggerWebhookEvent.getSourceRepoType());
  }
}
