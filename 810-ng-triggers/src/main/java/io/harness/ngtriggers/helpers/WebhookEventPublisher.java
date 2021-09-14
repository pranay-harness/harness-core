/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.helpers;

import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_REQUEST_PAYLOAD_DETAILS;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookTriggerType.GIT;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookEventType;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class WebhookEventPublisher {
  @Inject @Named(WEBHOOK_REQUEST_PAYLOAD_DETAILS) private Producer eventProducer;

  public void publishGitWebhookEvent(WebhookPayloadData webhookPayloadData, WebhookEventType webhookEventType) {
    WebhookDTO webhookDTO = WebhookDTO.newBuilder()
                                .setWebhookEventType(webhookEventType)
                                .setWebhookTriggerType(GIT)
                                .setJsonPayload(webhookPayloadData.getOriginalEvent().getPayload())
                                .setParsedResponse(webhookPayloadData.getParseWebhookResponse())
                                .build();

    eventProducer.send(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", webhookPayloadData.getOriginalEvent().getAccountId(),
                "correlationId", webhookPayloadData.getOriginalEvent().getUuid(), "sourceRepoType",
                webhookPayloadData.getOriginalEvent().getSourceRepoType()))
            .setData(webhookDTO.toByteString())
            .build());
  }
}
