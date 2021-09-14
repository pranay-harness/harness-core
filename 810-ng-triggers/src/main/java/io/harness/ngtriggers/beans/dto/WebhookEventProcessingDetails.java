/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class WebhookEventProcessingDetails {
  boolean eventFound;
  String eventId;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String triggerIdentifier;
  String pipelineIdentifier;
  String pipelineExecutionId;
  boolean exceptionOccured;
  String status;
  String message;
  String payload;
  Long eventCreatedAt;
  String runtimeInput;
}
