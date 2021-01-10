package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_TRIGGER_FOUND_FOR_REPO;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_CONDITIONS;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_REPO;

import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse;
import io.harness.ngtriggers.beans.scm.ParsePayloadResponse.ParsePayloadResponseBuilder;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.eventmapper.WebhookEventToTriggerMapper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtil;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitWebhookEventToTriggerMapper implements WebhookEventToTriggerMapper {
  private final NGTriggerService ngTriggerService;
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    // 1. Parse Payload
    ParsePayloadResponse parsePayloadResponse = convertWebhookResponse(triggerWebhookEvent);
    if (parsePayloadResponse.isExceptionOccured()) {
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.prepareResponseForScmException(parsePayloadResponse))
          .build();
    }

    WebhookPayloadData webhookPayloadData = parsePayloadResponse.getWebhookPayloadData();
    // 2. Get Trigger for Repo
    List<NGTriggerEntity> triggersForRepo =
        retrieveTriggersConfiguredForRepo(triggerWebhookEvent, webhookPayloadData.getRepository().getLink());
    if (isEmpty(triggersForRepo)) {
      log.info("No trigger found for repoUrl:" + webhookPayloadData.getRepository().getLink());
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_REPO, triggerWebhookEvent,
              null, null, "No Trigger was configured for Repo: " + webhookPayloadData.getRepository().getLink(), null))
          .build();
    }

    // Filtered out disabled triggers
    triggersForRepo = triggersForRepo.stream()
                          .filter(trigger -> trigger.getEnabled() == null || trigger.getEnabled())
                          .collect(Collectors.toList());

    if (isEmpty(triggersForRepo)) {
      log.info("No ENABLED trigger found for repoUrl:" + webhookPayloadData.getRepository().getLink());
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_ENABLED_TRIGGER_FOUND_FOR_REPO,
              triggerWebhookEvent, null, null,
              "No Trigger configured for Repo was in ENABLED status: " + webhookPayloadData.getRepository().getLink(),
              null))
          .build();
    }

    // 3. Apply Event, Action and Condition filters
    List<TriggerDetails> matchedTriggers = applyFilters(webhookPayloadData, triggersForRepo);
    if (isEmpty(matchedTriggers)) {
      log.info("No trigger matched payload after condition evaluation:");
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_CONDITIONS,
              triggerWebhookEvent, null, null, "No Trigger matched conditions for payload event", null))
          .build();
    }

    return WebhookEventMappingResponse.builder()
        .failedToFindTrigger(false)
        .triggers(matchedTriggers)
        .parseWebhookResponse(parsePayloadResponse.getWebhookPayloadData().getParseWebhookResponse())
        .build();
  }

  // Add error handling
  @VisibleForTesting
  ParsePayloadResponse convertWebhookResponse(TriggerWebhookEvent triggerWebhookEvent) {
    ParsePayloadResponseBuilder builder = ParsePayloadResponse.builder();
    try {
      WebhookPayloadData webhookPayloadData = webhookEventPayloadParser.parseEvent(triggerWebhookEvent);
      builder.webhookPayloadData(webhookPayloadData).build();
    } catch (Exception e) {
      builder.exceptionOccured(true)
          .exception(e)
          .webhookPayloadData(WebhookPayloadData.builder().originalEvent(triggerWebhookEvent).build())
          .build();
    }

    return builder.build();
  }

  List<NGTriggerEntity> retrieveTriggersConfiguredForRepo(TriggerWebhookEvent triggerWebhookEvent, String repoUrl) {
    Page<NGTriggerEntity> triggerPage =
        ngTriggerService.listWebhookTriggers(triggerWebhookEvent.getAccountId(), repoUrl, false, false);

    List<NGTriggerEntity> listOfTriggers = triggerPage.get().collect(Collectors.toList());
    return isEmpty(listOfTriggers) ? Collections.emptyList() : listOfTriggers;
  }

  @VisibleForTesting
  List<TriggerDetails> applyFilters(WebhookPayloadData webhookPayloadData, List<NGTriggerEntity> triggersForRepo) {
    List<TriggerDetails> matchedTriggers = new ArrayList<TriggerDetails>();

    for (NGTriggerEntity ngTriggerEntity : triggersForRepo) {
      NGTriggerConfig ngTriggerConfig = ngTriggerElementMapper.toTriggerConfig(ngTriggerEntity.getYaml());
      TriggerDetails triggerDetails =
          TriggerDetails.builder().ngTriggerConfig(ngTriggerConfig).ngTriggerEntity(ngTriggerEntity).build();
      if (checkTriggerEligibility(webhookPayloadData, triggerDetails)) {
        matchedTriggers.add(triggerDetails);
      }
    }
    return matchedTriggers;
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
}
