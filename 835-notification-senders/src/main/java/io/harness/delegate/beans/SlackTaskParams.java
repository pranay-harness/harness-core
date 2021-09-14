/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
public class SlackTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  List<String> slackWebhookUrls;
  String message;
  String notificationId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> compatibility = new ArrayList<>();
    if (!slackWebhookUrls.isEmpty()) {
      URI uri = null;
      try {
        uri = new URI(slackWebhookUrls.get(0));
        compatibility.add(HttpConnectionExecutionCapability.builder()
                              .host(uri.getHost())
                              .scheme(uri.getScheme())
                              .port(uri.getPort())
                              .build());
      } catch (URISyntaxException e) {
        log.error("Can't parse webhookurl as URI {}", notificationId, e);
      }
    }
    return compatibility;
  }
}
