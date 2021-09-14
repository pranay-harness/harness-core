/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.webhook.services.api;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.webhook.entities.WebhookEvent;

@OwnedBy(PIPELINE)
public interface WebhookService {
  WebhookEvent addEventToQueue(WebhookEvent webhookEvent);
}
