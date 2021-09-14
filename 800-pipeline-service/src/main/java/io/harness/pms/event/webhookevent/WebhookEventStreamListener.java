/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.event.webhookevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.triggers.webhook.service.TriggerWebhookExecutionServiceV2;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class WebhookEventStreamListener implements MessageListener {
  TriggerWebhookExecutionServiceV2 triggerWebhookExecutionServiceV2;

  @Inject
  public WebhookEventStreamListener(TriggerWebhookExecutionServiceV2 triggerWebhookExecutionServiceV2) {
    this.triggerWebhookExecutionServiceV2 = triggerWebhookExecutionServiceV2;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      try {
        WebhookDTO webhookDTO = WebhookDTO.parseFrom(message.getMessage().getData());
        triggerWebhookExecutionServiceV2.processEvent(webhookDTO);
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException("Exception in unpacking/processing of WebhookDTO event", e);
      }
    }
    return true;
  }
}
