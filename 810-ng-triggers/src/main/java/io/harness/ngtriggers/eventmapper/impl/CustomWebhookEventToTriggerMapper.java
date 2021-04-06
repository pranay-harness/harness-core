package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_PROJECT;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.eventmapper.WebhookEventToTriggerMapper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class CustomWebhookEventToTriggerMapper implements WebhookEventToTriggerMapper {
  private final NGTriggerService ngTriggerService;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    String projectId = new StringBuilder(triggerWebhookEvent.getAccountId())
                           .append('/')
                           .append(triggerWebhookEvent.getOrgIdentifier())
                           .append('/')
                           .append(triggerWebhookEvent.getProjectIdentifier())
                           .toString();

    List<NGTriggerEntity> ngTriggerEntities =
        ngTriggerService.findTriggersForCustomWehbook(triggerWebhookEvent, false, true);

    if (isEmpty(ngTriggerEntities)) {
      String msg = "No Custom trigger found for Project:" + projectId;
      log.info(msg);
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(
              NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_PROJECT, triggerWebhookEvent, null, null, msg, null))
          .build();
    }

    // Filtering Triggers based on webhookTokenAuthType(inline or ref)
    if (isEmpty(ngTriggerEntities)) {
      ngTriggerEntities =
          ngTriggerEntities.stream()
              .filter(ngTriggerEntity
                  -> "inline".equals(ngTriggerEntity.getMetadata().getWebhook().getCustom().getCustomAuthTokenType()))
              .collect(Collectors.toList());
    }

    List<TriggerDetails> triggerDetailEligible = new ArrayList<>();
    for (NGTriggerEntity ngTriggerEntity : ngTriggerEntities) {
      NGTriggerConfig ngTriggerConfig = ngTriggerElementMapper.toTriggerConfig(ngTriggerEntity.getYaml());
      TriggerDetails triggerDetails =
          TriggerDetails.builder().ngTriggerConfig(ngTriggerConfig).ngTriggerEntity(ngTriggerEntity).build();

      NGTriggerSpec spec = triggerDetails.getNgTriggerConfig().getSource().getSpec();
      WebhookTriggerSpec triggerSpec = ((WebhookTriggerConfig) spec).getSpec();

      if (WebhookTriggerFilterUtils.checkIfCustomPayloadConditionsMatch(triggerWebhookEvent.getPayload(), triggerSpec)
          && WebhookTriggerFilterUtils.checkIfCustomHeaderConditionsMatch(
              triggerWebhookEvent.getHeaders(), triggerSpec)) {
        triggerDetailEligible.add(triggerDetails);
      }
    }

    if (isEmpty(triggerDetailEligible)) {
      String msg = "No trigger matched payload after condition evaluation for project: " + projectId;
      log.info(msg);
      return WebhookEventMappingResponse.builder()
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(
              NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS, triggerWebhookEvent, null, null, msg, null))
          .build();
    }

    return WebhookEventMappingResponse.builder()
        .isCustomTrigger(true)
        .failedToFindTrigger(false)
        .triggers(triggerDetailEligible)
        .build();
  }
}
