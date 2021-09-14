/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.triggers.webhook.service;

import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;

public interface TriggerWebhookExecutionServiceV2 {
  void processEvent(WebhookDTO webhookDTO);
}
