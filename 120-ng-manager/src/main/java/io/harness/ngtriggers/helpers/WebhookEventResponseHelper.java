package io.harness.ngtriggers.helpers;

import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WebhookEventResponseHelper {
  public WebhookEventResponse toResponse(FinalStatus status, TriggerWebhookEvent triggerWebhookEvent,
      NGPipelineExecutionResponseDTO pipelineExecutionResponseDTO) {
    WebhookEventResponse response = WebhookEventResponse.builder()
                                        .accountId(triggerWebhookEvent.getAccountId())
                                        .eventCorrelationId(triggerWebhookEvent.getUuid())
                                        .payload(triggerWebhookEvent.getPayload())
                                        .createdAt(triggerWebhookEvent.getCreatedAt())
                                        .finalStatus(status)
                                        .build();
    if (pipelineExecutionResponseDTO == null) {
      response.setExceptionOccurred(true);
      return response;
    }
    response.setPlanExecutionId(pipelineExecutionResponseDTO.getPlanExecution().getUuid());
    response.setExceptionOccurred(false);
    return response;
  }
}
