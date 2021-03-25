package io.harness.ngtriggers.eventmapper.filters;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.EXCEPTION_WHILE_PROCESSING;

import static java.util.Collections.emptyList;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;

import java.util.List;

public interface TriggerFilter {
  WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData);

  default WebhookEventMappingResponse getWebhookResponseForException(FilterRequestData filterRequestData, Exception e) {
    return WebhookEventMappingResponse.builder()
        .failedToFindTrigger(true)
        .webhookEventResponse(WebhookEventResponseHelper.toResponse(EXCEPTION_WHILE_PROCESSING,
            filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
            new StringBuilder(256)
                .append("Exception occurred while Processing Filter: ")
                .append(getClass().getSimpleName())
                .append(", for Project: ")
                .append(filterRequestData.getProjectFqn())
                .append(". ExceptionMessage: ")
                .append(e.getMessage())
                .toString(),
            null))
        .build();
  }

  default void addDetails(WebhookEventMappingResponseBuilder webhookEventMappingResponseBuilder,
      FilterRequestData filterRequestData, List<TriggerDetails> detailsList) {
    webhookEventMappingResponseBuilder.parseWebhookResponse(
        filterRequestData.getWebhookPayloadData().getParseWebhookResponse());

    if (hasNone(detailsList)) {
      webhookEventMappingResponseBuilder.failedToFindTrigger(true);
      webhookEventMappingResponseBuilder.triggers(emptyList());
    } else {
      webhookEventMappingResponseBuilder.failedToFindTrigger(false);
      webhookEventMappingResponseBuilder.triggers(detailsList);
    }
  }
}
