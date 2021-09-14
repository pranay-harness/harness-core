/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.beans.PRWebhookEvent;
import io.harness.beans.WebhookPayload;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequestHook;

import java.util.List;

@OwnedBy(DX)
public interface WebhookParserSCMService {
  ParseWebhookResponse parseWebhookUsingSCMAPI(List<HeaderConfig> headers, String payload);

  WebhookPayload parseWebhookPayload(ParseWebhookResponse parseWebhookResponse);

  PRWebhookEvent convertPRWebhookEvent(PullRequestHook prHook);
}
