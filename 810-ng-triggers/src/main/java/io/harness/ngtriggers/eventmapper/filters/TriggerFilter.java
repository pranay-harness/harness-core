package io.harness.ngtriggers.eventmapper.filters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.EXCEPTION_WHILE_PROCESSING;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;

import java.util.List;

@OwnedBy(PIPELINE)
public interface TriggerFilter {
  WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData);

  default WebhookEventMappingResponseBuilder initWebhookEventMappingResponse(FilterRequestData filterRequestData) {
    return WebhookEventMappingResponse.builder().isCustomTrigger(filterRequestData.isCustomTrigger());
  }

  default WebhookEventMappingResponse getWebhookResponseForException(FilterRequestData filterRequestData, Exception e) {
    return WebhookEventMappingResponse.builder()
        .failedToFindTrigger(true)
        .isCustomTrigger(filterRequestData.isCustomTrigger())
        .webhookEventResponse(WebhookEventResponseHelper.toResponse(EXCEPTION_WHILE_PROCESSING,
            filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
            new StringBuilder(256)
                .append("Exception occurred while Processing Trigger Filter: ")
                .append(getClass().getSimpleName())
                .append(", for Account: ")
                .append(filterRequestData.getWebhookPayloadData().getOriginalEvent().getAccountId())
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

    if (isEmpty(detailsList)) {
      webhookEventMappingResponseBuilder.failedToFindTrigger(true);
      webhookEventMappingResponseBuilder.triggers(emptyList());
    } else {
      webhookEventMappingResponseBuilder.failedToFindTrigger(false);
      webhookEventMappingResponseBuilder.triggers(detailsList);
    }
  }
}
