package io.harness.helpers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;

import io.harness.beans.EmbeddedUser;
import io.harness.exception.TriggerException;
import io.harness.execution.PlanExecution;
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
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import com.google.inject.Inject;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class TriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final PipelineExecuteHelper pipelineExecuteHelper;
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  private final WebhookEventToTriggerMapper webhookEventToTriggerMapper;
  private final PMSPipelineService pmsPipelineService;

  public static final String WEBHOOK_EVENT_PAYLOAD_EVENT_REPO_TYPE = "webhookEventRepoType";
  public static final String WEBHOOK_EVENT_PAYLOAD_EVENT_TYPE = "webhookEventType";

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

      contextAttributes.put(SetupAbstractionKeys.eventPayload, triggerWebhookEvent.getPayload());
      WebhookEventHeaderData webhookEventHeaderData =
          webhookEventPayloadParser.obtainWebhookSourceKeyData(triggerWebhookEvent.getHeaders());
      if (webhookEventHeaderData.isDataFound()) {
        contextAttributes.put(WEBHOOK_EVENT_PAYLOAD_EVENT_REPO_TYPE, webhookEventHeaderData.getSourceKey());
        contextAttributes.put(WEBHOOK_EVENT_PAYLOAD_EVENT_TYPE, webhookEventHeaderData.getSourceKeyVal().get(0));
      }

      PlanExecution response = resolveRuntimeInputAndSubmitExecutionRequest(triggerDetails, contextAttributes);
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

  private PlanExecution resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, Map<String, Object> contextAttributes) {
    EmbeddedUser embeddedUser = EmbeddedUser.builder().email("").name("trigger").uuid("systemUser").build();
    try {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      String targetIdentifier = ngTriggerEntity.getTargetIdentifier();
      Optional<PipelineEntity> pipelineEntityToExecute = pmsPipelineService.get(ngTriggerEntity.getAccountId(),
          ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier(), targetIdentifier, false);

      if (pipelineEntityToExecute.get() == null) {
        throw new TriggerException(new StringBuilder(128)
                                       .append("Unable to continue trigger execution. Pipeline with identifier: ")
                                       .append(ngTriggerEntity.getTargetIdentifier())
                                       .append(", with org: ")
                                       .append(ngTriggerEntity.getOrgIdentifier())
                                       .append(", with ProjectId: ")
                                       .append(ngTriggerEntity.getProjectIdentifier())
                                       .append(", For Trigger: ")
                                       .append(ngTriggerEntity.getIdentifier())
                                       .append(" does not exists. ")
                                       .toString(),
            USER);
      }

      String runtimeInputYaml = readRuntimeInputFromConfig(triggerDetails.getNgTriggerConfig());

      String pipelineYaml = pipelineEntityToExecute.get().getYaml();

      String sanitizedRuntimeInputYaml = MergeHelper.sanitizeRuntimeInput(pipelineYaml, runtimeInputYaml);
      contextAttributes.put(SetupAbstractionKeys.inputSetYaml, sanitizedRuntimeInputYaml);

      String finalPipelineYmlForTrigger =
          MergeHelper.mergeInputSetIntoPipeline(pipelineYaml, sanitizedRuntimeInputYaml);

      return pipelineExecuteHelper.startExecution(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
          ngTriggerEntity.getProjectIdentifier(), finalPipelineYmlForTrigger, embeddedUser, contextAttributes);
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
