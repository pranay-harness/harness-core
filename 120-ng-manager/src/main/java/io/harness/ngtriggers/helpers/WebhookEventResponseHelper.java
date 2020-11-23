package io.harness.ngtriggers.helpers;

import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus;

import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WebhookEventResponseHelper {
  public WebhookEventResponse toResponse(FinalStatus status, TriggerWebhookEvent triggerWebhookEvent,
      NGPipelineExecutionResponseDTO pipelineExecutionResponseDTO, String triggerIdentifier) {
    WebhookEventResponse response = WebhookEventResponse.builder()
                                        .accountId(triggerWebhookEvent.getAccountId())
                                        .eventCorrelationId(triggerWebhookEvent.getUuid())
                                        .payload(triggerWebhookEvent.getPayload())
                                        .createdAt(triggerWebhookEvent.getCreatedAt())
                                        .finalStatus(status)
                                        .triggerIdentifier(triggerIdentifier)
                                        .build();
    if (pipelineExecutionResponseDTO == null) {
      response.setExceptionOccurred(true);
      return response;
    }
    response.setPlanExecutionId(pipelineExecutionResponseDTO.getPlanExecution().getUuid());
    response.setExceptionOccurred(false);
    return response;
  }

  public boolean isFinalStatusAnEvent(FinalStatus status) {
    Set<FinalStatus> set = EnumSet.of(FinalStatus.INVALID_RUNTIME_INPUT_YAML, FinalStatus.TARGET_DID_NOT_EXECUTE,
        FinalStatus.TARGET_EXECUTION_REQUESTED);
    return set.contains(status);
  }

  public TriggerEventHistory toEntity(WebhookEventResponse response) {
    return TriggerEventHistory.builder()
        .accountId(response.getAccountId())
        .eventCorrelationId(response.getEventCorrelationId())
        .payload(response.getPayload())
        .eventCreatedAt(response.getCreatedAt())
        .finalStatus(response.getFinalStatus())
        .message(response.getMessage())
        .planExecutionId(response.getPlanExecutionId())
        .exceptionOccurred(response.isExceptionOccurred())
        .triggerIdentifier(response.getTriggerIdentifier())
        .build();
  }
}
