package io.harness.ngtriggers.helpers;

import static io.harness.ngtriggers.Constants.X_BIT_BUCKET_EVENT;
import static io.harness.ngtriggers.Constants.X_GIT_HUB_EVENT;
import static io.harness.ngtriggers.Constants.X_GIT_LAB_EVENT;
import static io.harness.ngtriggers.Constants.X_HARNESS_CUSTOM_EVENT;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.eventmapper.impl.CustomWebhookEventToTriggerMapper;
import io.harness.ngtriggers.eventmapper.impl.GitWebhookEventToTriggerMapper;
import io.harness.ngtriggers.utils.WebhookEventPayloadParser;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class WebhookEventMapperHelper {
  private final WebhookEventPayloadParser webhookEventPayloadParser;
  private final GitWebhookEventToTriggerMapper gitWebhookEventToTriggerMapper;
  private final CustomWebhookEventToTriggerMapper customWebhookEventToTriggerMapper;

  public WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerWebhookEvent triggerWebhookEvent) {
    Set<String> headerKeys = triggerWebhookEvent.getHeaders().stream().map(HeaderConfig::getKey).collect(toSet());
    if (webhookEventPayloadParser.containsHeaderKey(headerKeys, X_HARNESS_CUSTOM_EVENT)) {
      return customWebhookEventToTriggerMapper.mapWebhookEventToTriggers(triggerWebhookEvent);
    }

    if (isGitWebhookEvent(headerKeys)) {
      return gitWebhookEventToTriggerMapper.mapWebhookEventToTriggers(triggerWebhookEvent);
    }

    return null;
  }

  private boolean isGitWebhookEvent(Set<String> headerKeys) {
    return webhookEventPayloadParser.containsHeaderKey(headerKeys, X_GIT_HUB_EVENT)
        || webhookEventPayloadParser.containsHeaderKey(headerKeys, X_GIT_LAB_EVENT)
        || webhookEventPayloadParser.containsHeaderKey(headerKeys, X_BIT_BUCKET_EVENT);
  }
}
