package io.harness.ngtriggers.helper;

import static io.harness.cdng.pipeline.plancreators.PipelinePlanCreator.WEBHOOK_EVENT_PAYLOAD_EVENT_REPO_TYPE;
import static io.harness.cdng.pipeline.plancreators.PipelinePlanCreator.WEBHOOK_EVENT_PAYLOAD_EVENT_TYPE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_DID_NOT_EXECUTE;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.cdng.pipeline.mappers.NGPipelineExecutionDTOMapper;
import io.harness.cdng.pipeline.plancreators.PipelinePlanCreator;
import io.harness.exception.TriggerException;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngpipeline.pipeline.mappers.PipelineDtoMapper;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.WebhookEventHeaderData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult.WebhookEventProcessingResultBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.TargetExecutionSummary;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.helpers.WebhookEventToTriggerMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  private final WebhookEventToTriggerMapper webhookEventToTriggerMapper;

  public WebhookEventProcessingResult handleTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
    WebhookEventMappingResponse webhookEventMappingResponse =
        webhookEventToTriggerMapper.mapWebhookEventToTriggers(triggerWebhookEvent);

    WebhookEventProcessingResultBuilder resultBuilder = WebhookEventProcessingResult.builder();
    List<WebhookEventResponse> eventResponses = new ArrayList<>();
    if (!webhookEventMappingResponse.isFailedToFindTrigger()) {
      log.info("Preparing for pipeline execution request");
      resultBuilder.mappedToTriggers(true);
      if (isNotEmpty(webhookEventMappingResponse.getTriggers())) {
        for (TriggerDetails triggerDetails : webhookEventMappingResponse.getTriggers()) {
          eventResponses.add(triggerPipelineExecution(triggerWebhookEvent, triggerDetails));
        }
      }
    } else {
      resultBuilder.mappedToTriggers(false);
      eventResponses.add(webhookEventMappingResponse.getWebhookEventResponse());
    }

    return resultBuilder.responses(eventResponses).build();
  }

  private WebhookEventResponse triggerPipelineExecution(
      TriggerWebhookEvent triggerWebhookEvent, TriggerDetails triggerDetails) {
    String runtimeInputYaml = null;
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();

    try {
      runtimeInputYaml =
          ((PipelineTargetSpec) triggerDetails.getNgTriggerConfig().getTarget().getSpec()).getRuntimeInputYaml();

      Map<String, Object> contextAttributes = new HashMap<>();
      contextAttributes.put(PipelinePlanCreator.EVENT_PAYLOAD_KEY, triggerWebhookEvent.getPayload());

      WebhookEventHeaderData webhookEventHeaderData =
          webhookEventPayloadParser.obtainWebhookSourceKeyData(triggerWebhookEvent.getHeaders());
      if (webhookEventHeaderData.isDataFound()) {
        contextAttributes.put(WEBHOOK_EVENT_PAYLOAD_EVENT_REPO_TYPE, webhookEventHeaderData.getSourceKey());
        contextAttributes.put(WEBHOOK_EVENT_PAYLOAD_EVENT_TYPE, webhookEventHeaderData.getSourceKeyVal().get(0));
      }

      NGPipelineExecutionResponseDTO response =
          resolveRuntimeInputAndSubmitExecutionRequest(triggerDetails, contextAttributes);

      TargetExecutionSummary targetExecutionSummary =
          WebhookEventResponseHelper.prepareTargetExecutionSummary(response, triggerDetails, runtimeInputYaml);
      if (response.isErrorResponse()) {
        log.warn(new StringBuilder(128)
                     .append(ngTriggerEntity.getTargetType())
                     .append(" execution failed to start : ")
                     .append(ngTriggerEntity.getTargetIdentifier())
                     .append(", using trigger: ")
                     .append(ngTriggerEntity.getIdentifier())
                     .toString());
        return WebhookEventResponseHelper.toResponse(
            TARGET_DID_NOT_EXECUTE, triggerWebhookEvent, response, ngTriggerEntity, EMPTY, targetExecutionSummary);
      } else {
        log.info(new StringBuilder(128)
                     .append(ngTriggerEntity.getTargetType())
                     .append(" execution was requested successfully for Pipeline: ")
                     .append(ngTriggerEntity.getTargetIdentifier())
                     .append(", using trigger: ")
                     .append(ngTriggerEntity.getIdentifier())
                     .toString());
        return WebhookEventResponseHelper.toResponse(TARGET_EXECUTION_REQUESTED, triggerWebhookEvent, response,
            ngTriggerEntity, "Pipeline execution was requested successfully", targetExecutionSummary);
      }
    } catch (Exception e) {
      log.info(new StringBuilder(128)
                   .append(" Exception occurred while requesting ")
                   .append(ngTriggerEntity.getTargetType())
                   .append(" execution. Identifier: ")
                   .append(ngTriggerEntity.getTargetIdentifier())
                   .append(", using trigger: ")
                   .append(ngTriggerEntity.getIdentifier())
                   .append(". Exception Message: ")
                   .append(e.getMessage())
                   .toString());
      TargetExecutionSummary targetExecutionSummary =
          WebhookEventResponseHelper.prepareTargetExecutionSummary(null, triggerDetails, runtimeInputYaml);
      return WebhookEventResponseHelper.toResponse(INVALID_RUNTIME_INPUT_YAML, triggerWebhookEvent, null,
          ngTriggerEntity, e.getMessage(), targetExecutionSummary);
    }
  }

  private NGPipelineExecutionResponseDTO resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, Map<String, Object> contextAttributes) {
    // TODO: once, we have user object availalbe, use it.
    EmbeddedUser embeddedUser = EmbeddedUser.builder().email("").name("trigger").uuid("systemUser").build();
    try {
      String finalPipelineYmlForTrigger = ngTriggerService.generateFinalPipelineYmlForTrigger(triggerDetails);

      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      String runtimeInputYaml = readRuntimeInputFromConfig(triggerDetails.getNgTriggerConfig());
      NgPipelineEntity ngPipelineEntity = PipelineDtoMapper.toPipelineEntity(ngTriggerEntity.getAccountId(),
          ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier(), finalPipelineYmlForTrigger);

      contextAttributes.put(PipelinePlanCreator.INPUT_SET_YAML_KEY, runtimeInputYaml);
      PlanExecution planExecution = ngPipelineExecuteHelper.startPipelinePlanExecution(ngTriggerEntity.getAccountId(),
          ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier(), ngPipelineEntity.getNgPipeline(),
          embeddedUser, contextAttributes);
      return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(
          planExecution, MergeInputSetResponse.builder().isErrorResponse(false).build());
    } catch (Exception e) {
      throw new TriggerException("Failed while requesting Pipeline Execution" + e.getMessage(), USER);
    }
  }

  private String readRuntimeInputFromConfig(NGTriggerConfig ngTriggerConfig) {
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
    return pipelineTargetSpec.getRuntimeInputYaml();
  }
}
