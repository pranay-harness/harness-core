/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.TriggerEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_JEXL_CONDITIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.TriggerEventResponseHelper;
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
public class JexlConditionsTriggerFilter implements TriggerFilter {
  private NGTriggerElementMapper ngTriggerElementMapper;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = initWebhookEventMappingResponse(filterRequestData);
    List<TriggerDetails> matchedTriggers = new ArrayList<>();

    for (TriggerDetails trigger : filterRequestData.getDetails()) {
      try {
        NGTriggerConfigV2 ngTriggerConfig = trigger.getNgTriggerConfigV2();
        if (ngTriggerConfig == null) {
          ngTriggerConfig = ngTriggerElementMapper.toTriggerConfigV2(trigger.getNgTriggerEntity().getYaml());
        }

        TriggerDetails triggerDetails = TriggerDetails.builder()
                                            .ngTriggerConfigV2(ngTriggerConfig)
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
      log.info("No trigger matched payload after jexl condition evaluation:");
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(TriggerEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_JEXL_CONDITIONS,
              filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
              "No Trigger matched jexl conditions for payload event for Project: " + filterRequestData.getAccountId(),
              null))
          .build();
    } else {
      addDetails(mappingResponseBuilder, filterRequestData, matchedTriggers);
    }
    return mappingResponseBuilder.build();
  }

  boolean checkTriggerEligibility(FilterRequestData filterRequestData, TriggerDetails triggerDetails) {
    NGTriggerSpecV2 spec = triggerDetails.getNgTriggerConfigV2().getSource().getSpec();
    if (!WebhookTriggerConfigV2.class.isAssignableFrom(spec.getClass())) {
      log.error("Trigger spec is not a WebhookTriggerConfig");
      return false;
    }

    WebhookTriggerConfigV2 webhookTriggerConfig = (WebhookTriggerConfigV2) spec;
    return WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
        filterRequestData.getWebhookPayloadData().getParseWebhookResponse(),
        filterRequestData.getWebhookPayloadData().getOriginalEvent().getHeaders(),
        filterRequestData.getWebhookPayloadData().getOriginalEvent().getPayload(),
        webhookTriggerConfig.getSpec().fetchPayloadAware().fetchJexlCondition());
  }
}
