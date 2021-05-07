package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_HEADER_CONDITIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV1;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV1;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerConfigV1;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookConfigHelper;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class HeaderTriggerFilter implements TriggerFilter {
  private NGTriggerElementMapper ngTriggerElementMapper;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = WebhookEventMappingResponse.builder();
    List<TriggerDetails> matchedTriggers = new ArrayList<>();

    for (TriggerDetails trigger : filterRequestData.getDetails()) {
      try {
        NGTriggerConfigV1 ngTriggerConfig = trigger.getNgTriggerConfigV1();
        if (ngTriggerConfig == null) {
          ngTriggerConfig = ngTriggerElementMapper.toTriggerConfig(trigger.getNgTriggerEntity().getYaml());
        }

        TriggerDetails triggerDetails = TriggerDetails.builder()
                                            .ngTriggerConfigV1(ngTriggerConfig)
                                            .ngTriggerEntity(trigger.getNgTriggerEntity())
                                            .build();
        if (checkTriggerEligibility(filterRequestData, triggerDetails)) {
          matchedTriggers.add(triggerDetails);
        }
      } catch (Exception e) {
        log.error(getTriggerSkipMessage(trigger.getNgTriggerEntity()), e);
      }
    }

    if (isEmpty(matchedTriggers)) {
      log.info("No trigger matched payload after condition evaluation:");
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_HEADER_CONDITIONS,
              filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
              "No Trigger matched conditions for payload event for Account: " + filterRequestData.getAccountId(), null))
          .build();
    } else {
      addDetails(mappingResponseBuilder, filterRequestData, matchedTriggers);
    }
    return mappingResponseBuilder.build();
  }

  boolean checkTriggerEligibility(FilterRequestData filterRequestData, TriggerDetails triggerDetails) {
    NGTriggerSpecV1 spec = triggerDetails.getNgTriggerConfigV1().getSource().getSpec();
    if (!WebhookTriggerConfigV1.class.isAssignableFrom(spec.getClass())) {
      log.error("Trigger spec is not a WebhookTriggerConfig");
      return false;
    }

    WebhookTriggerConfigV1 webhookTriggerConfigV1 = (WebhookTriggerConfigV1) spec;
    return WebhookTriggerFilterUtils.checkIfCustomHeaderConditionsMatch(
        filterRequestData.getWebhookPayloadData().getOriginalEvent().getHeaders(),
        WebhookConfigHelper.retrieveHeaderConditions(webhookTriggerConfigV1));
  }
}
