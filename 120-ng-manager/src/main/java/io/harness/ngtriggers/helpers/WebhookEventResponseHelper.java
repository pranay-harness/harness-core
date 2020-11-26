package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.INVALID_PAYLOAD;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.SCM_SERVICE_CONNECTION_FAILED;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.TARGET_DID_NOT_EXECUTE;
import static io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.target.pipeline.TargetExecutionSummary;
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
      String triggerIdentifier, String message, TargetExecutionSummary targetExecutionSummary) {
    WebhookEventResponse response = WebhookEventResponse.builder()
                                        .accountId(triggerWebhookEvent.getAccountId())
                                        .eventCorrelationId(triggerWebhookEvent.getUuid())
                                        .payload(triggerWebhookEvent.getPayload())
                                        .createdAt(triggerWebhookEvent.getCreatedAt())
                                        .finalStatus(status)
                                        .triggerIdentifier(triggerIdentifier)
                                        .message(message)
                                        .targetExecutionSummary(targetExecutionSummary)
                                        .build();
    if (pipelineExecutionResponseDTO == null) {
      response.setExceptionOccurred(true);
      return response;
    }
    response.setExceptionOccurred(false);
    return response;
  }

  public boolean isFinalStatusAnEvent(
      io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus status) {
    Set<WebhookEventResponse.FinalStatus> set = EnumSet.of(INVALID_RUNTIME_INPUT_YAML, TARGET_DID_NOT_EXECUTE,
        TARGET_EXECUTION_REQUESTED, NO_MATCHING_TRIGGER_FOR_REPO, NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS);
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
        .exceptionOccurred(response.isExceptionOccurred())
        .triggerIdentifier(response.getTriggerIdentifier())
        .targetExecutionSummary(response.getTargetExecutionSummary())
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
    return toResponse(status, parsePayloadReponse.getOriginalEvent(), null, EMPTY, exception.getMessage(), null);
  }

  public TargetExecutionSummary prepareTargetExecutionSummary(
      NGPipelineExecutionResponseDTO ngPipelineExecutionResponseDTO, TriggerDetails triggerDetails,
      String runtimeInputYaml) {
    if (ngPipelineExecutionResponseDTO == null) {
      return TargetExecutionSummary.builder()
          .triggerId(triggerDetails.getNgTriggerEntity().getIdentifier())
          .targetId(triggerDetails.getNgTriggerEntity().getTargetIdentifier())
          .runtimeInput(runtimeInputYaml)
          .build();
    } else {
      return TargetExecutionSummary.builder()
          .targetId(triggerDetails.getNgTriggerEntity().getTargetIdentifier())
          .planExecutionId(ngPipelineExecutionResponseDTO.getPlanExecution().getUuid())
          .executionStatus(ngPipelineExecutionResponseDTO.getPlanExecution().getStatus().name())
          .triggerId(triggerDetails.getNgTriggerEntity().getIdentifier())
          .runtimeInput(runtimeInputYaml)
          .startTs(ngPipelineExecutionResponseDTO.getPlanExecution().getStartTs())
          .build();
    }
  }
}
