/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.core.trigger;

import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.inputset.WebhookTriggerExecutionInputSet;
import io.harness.ci.beans.entities.BuildNumberDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

@Singleton
public class WebhookTriggerProcessor implements TriggerProcessor {
  @Inject WebhookTriggerProcessorUtils webhookTriggerProcessorUtils;

  @Override
  public void validateTriggerCondition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void validateTriggerAction() {
    throw new UnsupportedOperationException();
  }

  public CIExecutionArgs generateExecutionArgs(
      String pipelineId, String eventPayload, HttpHeaders httpHeaders, BuildNumberDetails buildNumberDetails) {
    // TODO Add branch if require

    WebhookExecutionSource webhookExecutionSource =
        webhookTriggerProcessorUtils.fetchWebhookExecutionSource(eventPayload, httpHeaders);
    return CIExecutionArgs.builder()
        .executionSource(webhookExecutionSource)
        .inputSet(WebhookTriggerExecutionInputSet.builder().payload(eventPayload).build())
        .buildNumberDetails(buildNumberDetails)
        .build();
  }
}
