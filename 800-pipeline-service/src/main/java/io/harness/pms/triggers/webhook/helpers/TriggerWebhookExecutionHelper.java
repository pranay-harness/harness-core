package io.harness.pms.triggers.webhook.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;
import static io.harness.pms.contracts.triggers.Type.CUSTOM;
import static io.harness.pms.contracts.triggers.Type.GIT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.TriggerMappingRequestData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult.WebhookEventProcessingResultBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.TargetExecutionSummary;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.helpers.WebhookEventMapperHelper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.TriggerPayload.Builder;
import io.harness.pms.triggers.TriggerExecutionHelper;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class TriggerWebhookExecutionHelper {
  private final WebhookEventMapperHelper webhookEventMapperHelper;
  private final TriggerExecutionHelper triggerExecutionHelper;

  public WebhookEventProcessingResult handleTriggerWebhookEvent(TriggerMappingRequestData mappingRequestData) {
    WebhookEventMappingResponse webhookEventMappingResponse =
        webhookEventMapperHelper.mapWebhookEventToTriggers(mappingRequestData);

    TriggerWebhookEvent triggerWebhookEvent = mappingRequestData.getTriggerWebhookEvent();
    WebhookEventProcessingResultBuilder resultBuilder = WebhookEventProcessingResult.builder();
    List<WebhookEventResponse> eventResponses = new ArrayList<>();
    if (!webhookEventMappingResponse.isFailedToFindTrigger()) {
      log.info("Preparing for pipeline execution request");
      resultBuilder.mappedToTriggers(true);
      if (isNotEmpty(webhookEventMappingResponse.getTriggers())) {
        for (TriggerDetails triggerDetails : webhookEventMappingResponse.getTriggers()) {
          eventResponses.add(triggerPipelineExecution(triggerWebhookEvent, triggerDetails,
              getTriggerPayload(webhookEventMappingResponse, triggerWebhookEvent.getPayload())));
        }
      }
    } else {
      resultBuilder.mappedToTriggers(false);
      eventResponses.add(webhookEventMappingResponse.getWebhookEventResponse());
    }

    return resultBuilder.responses(eventResponses).build();
  }

  private TriggerPayload getTriggerPayload(
      WebhookEventMappingResponse webhookEventMappingResponse, String jsonPayload) {
    Builder builder = TriggerPayload.newBuilder().setJsonPayload(jsonPayload);

    if (webhookEventMappingResponse.isCustomTrigger()) {
      return builder.setType(CUSTOM).build();
    }

    ParseWebhookResponse parseWebhookResponse = webhookEventMappingResponse.getParseWebhookResponse();
    if (parseWebhookResponse.hasPr()) {
      builder.setParsedPayload(ParsedPayload.newBuilder().setPr(parseWebhookResponse.getPr()).build()).build();
    } else {
      builder.setParsedPayload(ParsedPayload.newBuilder().setPush(parseWebhookResponse.getPush()).build()).build();
    }

    return builder.setType(GIT).build();
  }

  private WebhookEventResponse triggerPipelineExecution(
      TriggerWebhookEvent triggerWebhookEvent, TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    String runtimeInputYaml = null;
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    try {
      runtimeInputYaml =
          ((PipelineTargetSpec) triggerDetails.getNgTriggerConfig().getTarget().getSpec()).getRuntimeInputYaml();

      PlanExecution response = triggerExecutionHelper.resolveRuntimeInputAndSubmitExecutionRequest(
          triggerDetails, triggerPayload, triggerWebhookEvent);
      TargetExecutionSummary targetExecutionSummary =
          WebhookEventResponseHelper.prepareTargetExecutionSummary(response, triggerDetails, runtimeInputYaml);

      log.info(ngTriggerEntity.getTargetType() + " execution was requested successfully for Pipeline: "
          + ngTriggerEntity.getTargetIdentifier() + ", using trigger: " + ngTriggerEntity.getIdentifier());

      return WebhookEventResponseHelper.toResponse(TARGET_EXECUTION_REQUESTED, triggerWebhookEvent, ngTriggerEntity,
          "Pipeline execution was requested successfully", targetExecutionSummary);
    } catch (Exception e) {
      log.info(" Exception occurred while requesting " + ngTriggerEntity.getTargetType()
          + " execution. Identifier: " + ngTriggerEntity.getTargetIdentifier()
          + ", using trigger: " + ngTriggerEntity.getIdentifier() + ". Exception Message: " + e.getMessage());

      TargetExecutionSummary targetExecutionSummary = WebhookEventResponseHelper.prepareTargetExecutionSummary(
          (PlanExecution) null, triggerDetails, runtimeInputYaml);
      return WebhookEventResponseHelper.toResponse(INVALID_RUNTIME_INPUT_YAML, triggerWebhookEvent, null,
          ngTriggerEntity, e.getMessage(), targetExecutionSummary);
    }
  }
}
