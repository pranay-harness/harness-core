package io.harness.ngtriggers.eventmapper.impl;

import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.eventmapper.WebhookEventToTriggerMapper;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CustomWebhookEventToTriggerMapper implements WebhookEventToTriggerMapper {
  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    return WebhookEventMappingResponse.builder()
        .isCustomTrigger(true)
        .failedToFindTrigger(false)
        .triggers(null)
        .build();
  }
}
