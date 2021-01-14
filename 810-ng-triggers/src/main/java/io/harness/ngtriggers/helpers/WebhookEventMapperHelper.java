package io.harness.ngtriggers.helpers;

import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
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

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    if (triggerWebhookEvent.getSourceRepoType() == WebhookSourceRepo.CUSTOM.name()) {
      return customWebhookEventToTriggerMapper.mapWebhookEventToTriggers(triggerWebhookEvent);
    }

    return gitWebhookEventToTriggerMapper.mapWebhookEventToTriggers(triggerWebhookEvent);
  }
}
