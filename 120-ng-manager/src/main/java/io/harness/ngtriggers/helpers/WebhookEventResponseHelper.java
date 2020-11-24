package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.INVALID_PAYLOAD;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.SCM_SERVICE_CONNECTION_FAILED;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.TARGET_DID_NOT_EXECUTE;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WebhookEventResponseHelper {
  public WebhookEventResponse toResponse(WebhookEventResponse.FinalStatus status,
      TriggerWebhookEvent triggerWebhookEvent, NGPipelineExecutionResponseDTO pipelineExecutionResponseDTO,
      String triggerIdentifier, String message) {
    WebhookEventResponse response = WebhookEventResponse.builder()
                                        .accountId(triggerWebhookEvent.getAccountId())
                                        .eventCorrelationId(triggerWebhookEvent.getUuid())
                                        .payload(triggerWebhookEvent.getPayload())
                                        .createdAt(triggerWebhookEvent.getCreatedAt())
                                        .finalStatus(status)
                                        .triggerIdentifier(triggerIdentifier)
                                        .message(message)
                                        .build();
    if (pipelineExecutionResponseDTO == null) {
      response.setExceptionOccurred(true);
      return response;
    }
    response.setPlanExecutionId(pipelineExecutionResponseDTO.getPlanExecution().getUuid());
    response.setExceptionOccurred(false);
    return response;
  }

  public boolean isFinalStatusAnEvent(
      io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus status) {
    Set<WebhookEventResponse.FinalStatus> set =
        EnumSet.of(INVALID_RUNTIME_INPUT_YAML, TARGET_DID_NOT_EXECUTE, TARGET_EXECUTION_REQUESTED);
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

  public WebhookEventResponse prepareResponseForScmException(ParsePayloadResponse parsePayloadReponse) {
    WebhookEventResponse.FinalStatus status = INVALID_PAYLOAD;
    Exception exception = parsePayloadReponse.getException();
    if (StatusRuntimeException.class.isAssignableFrom(exception.getClass())) {
      StatusRuntimeException e = (StatusRuntimeException) exception;

      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        status = SCM_SERVICE_CONNECTION_FAILED;
      }
    }
    return toResponse(status, parsePayloadReponse.getOriginalEvent(), null, EMPTY, exception.getMessage());
  }
}
