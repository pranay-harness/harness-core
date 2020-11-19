package io.harness.ngtriggers.helpers;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerWebhookExecutionHelper {
  private final NGTriggerService ngTriggerService;
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;
  private final WebhookEventPayloadParser webhookEventPayloadParser;

  public boolean handleTriggerWebhookEvent(TriggerWebhookEvent triggerWebhookEvent) {
    WebhookPayloadData webhookPayloadData = parseEventData(triggerWebhookEvent);

    List<NGTriggerEntity> triggersForRepo = retrieveTriggersConfiguredForRepo(triggerWebhookEvent, webhookPayloadData);
    if (EmptyPredicate.isEmpty(triggersForRepo)) {
      return true;
    }
    Optional<NGTriggerEntity> optionalEntity = applyFilters(webhookPayloadData, triggersForRepo);
    if (optionalEntity.isPresent()) {
      NGTriggerEntity entity = optionalEntity.get();
      try {
        NGPipelineExecutionResponseDTO response =
            resolveRuntimeInputAndSubmitExecutionRequest(entity, triggerWebhookEvent.getPayload());
        return !response.isErrorResponse();
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  // Add error handling
  private WebhookPayloadData parseEventData(TriggerWebhookEvent triggerWebhookEvent) {
    return webhookEventPayloadParser.parseEvent(triggerWebhookEvent);
  }

  private List<NGTriggerEntity> retrieveTriggersConfiguredForRepo(
      TriggerWebhookEvent triggerWebhookEvent, WebhookPayloadData webhookPayloadData) {
    String repoUrl = webhookPayloadData.getRepository().getLink();
    Page<NGTriggerEntity> triggerPage =
        ngTriggerService.listWebhookTriggers(triggerWebhookEvent.getAccountId(), repoUrl, false);

    List<NGTriggerEntity> listOfTriggers = triggerPage.get().collect(Collectors.toList());
    return EmptyPredicate.isEmpty(listOfTriggers) ? Collections.emptyList() : listOfTriggers;
  }

  @VisibleForTesting
  Optional<NGTriggerEntity> applyFilters(WebhookPayloadData webhookPayloadData, List<NGTriggerEntity> triggersForRepo) {
    NGTriggerEntity matchedTrigger = null;
    for (NGTriggerEntity ngTriggerEntity : triggersForRepo) {
      NGTriggerConfig ngTriggerConfig = NGTriggerElementMapper.toTriggerConfig(ngTriggerEntity.getYaml());
      if (checkTriggerEligibility(webhookPayloadData, ngTriggerEntity, ngTriggerConfig)) {
        if (matchedTrigger != null) {
          throw new InvalidRequestException("More than one trigger matched the eligibility criteria");
        }
        matchedTrigger = ngTriggerEntity;
      }
    }
    if (matchedTrigger == null) {
      return Optional.empty();
    }
    return Optional.of(matchedTrigger);
  }

  private boolean checkTriggerEligibility(
      WebhookPayloadData webhookPayloadData, NGTriggerEntity ngTriggerEntity, NGTriggerConfig ngTriggerConfig) {
    try {
      NGTriggerSpec spec = ngTriggerConfig.getSource().getSpec();
      if (!WebhookTriggerConfig.class.isAssignableFrom(spec.getClass())) {
        log.error("Trigger spec is not a WebhookTriggerConfig");
        return false;
      }

      WebhookTriggerSpec triggerSpec = ((WebhookTriggerConfig) spec).getSpec();
      return WebhookTriggerFilterUtil.evaluateFilterConditions(webhookPayloadData, triggerSpec);
    } catch (Exception e) {
      log.error("Failed while evaluating Trigger: " + ngTriggerEntity.getIdentifier()
          + ", For Account: " + ngTriggerEntity.getAccountId()
          + ", correlationId for event is: " + webhookPayloadData.getOriginalEvent().getUuid());
      return false;
    }
  }

  private NGPipelineExecutionResponseDTO resolveRuntimeInputAndSubmitExecutionRequest(
      NGTriggerEntity trigger, String eventPayload) {
    NGTriggerConfig ngTriggerConfig = NGTriggerElementMapper.toTriggerConfig(trigger.getYaml());
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    EmbeddedUser embeddedUser = EmbeddedUser.builder().email("email").name("name").uuid("uuid").build();

    if (PipelineTargetSpec.class.isAssignableFrom(targetSpec.getClass())) {
      PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
      return ngPipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(trigger.getAccountId(),
          trigger.getOrgIdentifier(), trigger.getProjectIdentifier(), trigger.getTargetIdentifier(),
          pipelineTargetSpec.getRuntimeInputYaml(), eventPayload, false, embeddedUser);
    }
    throw new InvalidRequestException("Target type does not match");
  }
}
