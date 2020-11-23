package io.harness.ngtriggers.helpers;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse.FinalStatus;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;
  private final WebhookEventPayloadParser webhookEventPayloadParser;

  public WebhookEventResponse handleTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
    WebhookPayloadData webhookPayloadData;
    try {
      webhookPayloadData = parseEventData(triggerWebhookEvent);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        return WebhookEventResponseHelper.toResponse(FinalStatus.SCM_SERVICE_DOWN, triggerWebhookEvent, null, "");
      }
      return WebhookEventResponseHelper.toResponse(FinalStatus.INVALID_PAYLOAD, triggerWebhookEvent, null, "");
    }

    List<NGTriggerEntity> triggersForRepo =
        retrieveTriggersConfiguredForRepo(triggerWebhookEvent, webhookPayloadData.getRepository().getLink());
    if (EmptyPredicate.isEmpty(triggersForRepo)) {
      return WebhookEventResponseHelper.toResponse(FinalStatus.NO_MATCHING_TRIGGER, triggerWebhookEvent, null, "");
    }
    Optional<TriggerDetails> optionalEntity = applyFilters(webhookPayloadData, triggersForRepo);
    if (optionalEntity.isPresent()) {
      TriggerDetails triggerDetails = optionalEntity.get();
      String triggerIdentifier = triggerDetails.getNgTriggerEntity().getIdentifier();
      try {
        NGPipelineExecutionResponseDTO response =
            resolveRuntimeInputAndSubmitExecutionRequest(triggerDetails, triggerWebhookEvent.getPayload());
        if (response.isErrorResponse()) {
          return WebhookEventResponseHelper.toResponse(
              FinalStatus.TARGET_DID_NOT_EXECUTE, triggerWebhookEvent, response, triggerIdentifier);
        } else {
          return WebhookEventResponseHelper.toResponse(
              FinalStatus.TARGET_EXECUTION_REQUESTED, triggerWebhookEvent, response, triggerIdentifier);
        }
      } catch (Exception e) {
        return WebhookEventResponseHelper.toResponse(
            FinalStatus.INVALID_RUNTIME_INPUT_YAML, triggerWebhookEvent, null, triggerIdentifier);
      }
    }
    return WebhookEventResponseHelper.toResponse(FinalStatus.NO_MATCHING_TRIGGER, triggerWebhookEvent, null, "");
  }

  // Add error handling
  private WebhookPayloadData parseEventData(TriggerWebhookEvent triggerWebhookEvent) {
    return webhookEventPayloadParser.parseEvent(triggerWebhookEvent);
  }

  private List<NGTriggerEntity> retrieveTriggersConfiguredForRepo(
      TriggerWebhookEvent triggerWebhookEvent, String repoUrl) {
    Page<NGTriggerEntity> triggerPage =
        ngTriggerService.listWebhookTriggers(triggerWebhookEvent.getAccountId(), repoUrl, false);

    List<NGTriggerEntity> listOfTriggers = triggerPage.get().collect(Collectors.toList());
    return EmptyPredicate.isEmpty(listOfTriggers) ? Collections.emptyList() : listOfTriggers;
  }

  @VisibleForTesting
  Optional<TriggerDetails> applyFilters(WebhookPayloadData webhookPayloadData, List<NGTriggerEntity> triggersForRepo) {
    TriggerDetails matchedTrigger = null;
    for (NGTriggerEntity ngTriggerEntity : triggersForRepo) {
      NGTriggerConfig ngTriggerConfig = NGTriggerElementMapper.toTriggerConfig(ngTriggerEntity.getYaml());
      TriggerDetails triggerDetails =
          TriggerDetails.builder().ngTriggerConfig(ngTriggerConfig).ngTriggerEntity(ngTriggerEntity).build();
      if (checkTriggerEligibility(webhookPayloadData, triggerDetails)) {
        if (matchedTrigger != null) {
          throw new InvalidRequestException("More than one trigger matched the eligibility criteria");
        }
        matchedTrigger = triggerDetails;
      }
    }
    if (matchedTrigger == null) {
      return Optional.empty();
    }
    return Optional.of(matchedTrigger);
  }

  private boolean checkTriggerEligibility(WebhookPayloadData webhookPayloadData, TriggerDetails triggerDetails) {
    try {
      NGTriggerSpec spec = triggerDetails.getNgTriggerConfig().getSource().getSpec();
      if (!WebhookTriggerConfig.class.isAssignableFrom(spec.getClass())) {
        log.error("Trigger spec is not a WebhookTriggerConfig");
        return false;
      }

      WebhookTriggerSpec triggerSpec = ((WebhookTriggerConfig) spec).getSpec();
      return WebhookTriggerFilterUtil.evaluateFilterConditions(webhookPayloadData, triggerSpec);
    } catch (Exception e) {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      log.error("Failed while evaluating Trigger: " + ngTriggerEntity.getIdentifier()
          + ", For Account: " + ngTriggerEntity.getAccountId()
          + ", correlationId for event is: " + webhookPayloadData.getOriginalEvent().getUuid());
      return false;
    }
  }

  private NGPipelineExecutionResponseDTO resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, String eventPayload) {
    NGTriggerConfig ngTriggerConfig = triggerDetails.getNgTriggerConfig();
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    EmbeddedUser embeddedUser = EmbeddedUser.builder().email("email").name("name").uuid("uuid").build();

    if (PipelineTargetSpec.class.isAssignableFrom(targetSpec.getClass())) {
      PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
      return ngPipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(ngTriggerEntity.getAccountId(),
          ngTriggerEntity.getOrgIdentifier(), ngTriggerEntity.getProjectIdentifier(),
          ngTriggerEntity.getTargetIdentifier(), pipelineTargetSpec.getRuntimeInputYaml(), eventPayload, false,
          embeddedUser);
    }
    throw new InvalidRequestException("Target type does not match");
  }
}
