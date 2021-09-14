/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo.CUSTOM;

import io.harness.ngtriggers.beans.dto.TriggerMappingRequestData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.eventmapper.impl.CustomWebhookEventToTriggerMapper;
import io.harness.ngtriggers.eventmapper.impl.GitWebhookEventToTriggerMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class WebhookEventMapperHelper {
  private final GitWebhookEventToTriggerMapper gitWebhookEventToTriggerMapper;
  private final CustomWebhookEventToTriggerMapper customWebhookEventToTriggerMapper;

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerMappingRequestData mappingRequestData) {
    TriggerWebhookEvent triggerWebhookEvent = mappingRequestData.getTriggerWebhookEvent();
    if (CUSTOM.name().equals(triggerWebhookEvent.getSourceRepoType())) {
      return customWebhookEventToTriggerMapper.mapWebhookEventToTriggers(mappingRequestData);
    }

    return gitWebhookEventToTriggerMapper.mapWebhookEventToTriggers(mappingRequestData);
  }
}
