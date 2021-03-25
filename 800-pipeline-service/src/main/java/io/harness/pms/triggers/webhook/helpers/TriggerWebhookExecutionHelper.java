package io.harness.pms.triggers.webhook.helpers;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.INVALID_RUNTIME_INPUT_YAML;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.TARGET_EXECUTION_REQUESTED;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK_CUSTOM;
import static io.harness.pms.contracts.triggers.Type.CUSTOM;
import static io.harness.pms.contracts.triggers.Type.GIT;

import io.harness.exception.TriggerException;
import io.harness.execution.PlanExecution;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventProcessingResult.WebhookEventProcessingResultBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.TargetExecutionSummary;
import io.harness.ngtriggers.beans.response.WebhookEventResponse;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.helpers.WebhookEventMapperHelper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.TriggerPayload.Builder;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class TriggerWebhookExecutionHelper {
  private final PipelineExecuteHelper pipelineExecuteHelper;
  private final WebhookEventMapperHelper webhookEventMapperHelper;
  private final PMSPipelineService pmsPipelineService;

  public WebhookEventProcessingResult handleTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
    WebhookEventMappingResponse webhookEventMappingResponse =
        webhookEventMapperHelper.mapWebhookEventToTriggers(triggerWebhookEvent);

    WebhookEventProcessingResultBuilder resultBuilder = WebhookEventProcessingResult.builder();
    List<WebhookEventResponse> eventResponses = new ArrayList<>();
    if (!webhookEventMappingResponse.isFailedToFindTrigger()) {
      log.info("Preparing for pipeline execution request");
      resultBuilder.mappedToTriggers(true);
      if (hasSome(webhookEventMappingResponse.getTriggers())) {
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

      PlanExecution response = resolveRuntimeInputAndSubmitExecutionRequest(triggerDetails, triggerPayload);
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
      TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    TriggeredBy embeddedUser = TriggeredBy.newBuilder().setIdentifier("trigger").setUuid("systemUser").build();

    TriggerType triggerType = findTriggerType(triggerPayload);
    ExecutionTriggerInfo triggerInfo =
        ExecutionTriggerInfo.newBuilder().setTriggerType(triggerType).setTriggeredBy(embeddedUser).build();
    try {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      String targetIdentifier = ngTriggerEntity.getTargetIdentifier();
      Optional<PipelineEntity> pipelineEntityToExecute =
          pmsPipelineService.incrementRunSequence(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
              ngTriggerEntity.getProjectIdentifier(), targetIdentifier, false);

      if (!pipelineEntityToExecute.isPresent()) {
        throw new TriggerException("Unable to continue trigger execution. Pipeline with identifier: "
                + ngTriggerEntity.getTargetIdentifier() + ", with org: " + ngTriggerEntity.getOrgIdentifier()
                + ", with ProjectId: " + ngTriggerEntity.getProjectIdentifier()
                + ", For Trigger: " + ngTriggerEntity.getIdentifier() + " does not exists. ",
            USER);
      }

      String runtimeInputYaml = readRuntimeInputFromConfig(triggerDetails.getNgTriggerConfig());

      ExecutionMetadata.Builder executionMetaDataBuilder =
          ExecutionMetadata.newBuilder()
              .setExecutionUuid(generateUuid())
              .setTriggerInfo(triggerInfo)
              .setRunSequence(pipelineEntityToExecute.get().getRunSequence())
              .setTriggerPayload(triggerPayload)
              .setPipelineIdentifier(pipelineEntityToExecute.get().getIdentifier());

      String pipelineYaml;
      if (hasNone(runtimeInputYaml)) {
        pipelineYaml = pipelineEntityToExecute.get().getYaml();
      } else {
        String pipelineYamlBeforeMerge = pipelineEntityToExecute.get().getYaml();
        String sanitizedRuntimeInputYaml = MergeHelper.sanitizeRuntimeInput(pipelineYamlBeforeMerge, runtimeInputYaml);
        if (hasNone(sanitizedRuntimeInputYaml)) {
          pipelineYaml = pipelineYamlBeforeMerge;
        } else {
          executionMetaDataBuilder.setInputSetYaml(sanitizedRuntimeInputYaml);
          pipelineYaml =
              MergeHelper.mergeInputSetIntoPipeline(pipelineYamlBeforeMerge, sanitizedRuntimeInputYaml, true);
        }
      }
      executionMetaDataBuilder.setYaml(pipelineYaml);

      return pipelineExecuteHelper.startExecution(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
          ngTriggerEntity.getProjectIdentifier(), pipelineYaml, executionMetaDataBuilder.build());
    } catch (Exception e) {
      throw new TriggerException("Failed while requesting Pipeline Execution" + e.getMessage(), USER);
    }
  }

  private TriggerType findTriggerType(TriggerPayload triggerPayload) {
    TriggerType triggerType = WEBHOOK;
    if (triggerPayload.getType() == CUSTOM) {
      triggerType = WEBHOOK_CUSTOM;
    } // cron will come here

    return triggerType;
  }

  private String readRuntimeInputFromConfig(NGTriggerConfig ngTriggerConfig) {
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
    return pipelineTargetSpec.getRuntimeInputYaml();
  }
}
