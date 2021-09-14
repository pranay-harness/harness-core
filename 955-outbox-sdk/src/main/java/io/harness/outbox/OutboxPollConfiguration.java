/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Value
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxPollConfiguration {
  int initialDelayInSeconds;
  int pollingIntervalInSeconds;
  int maximumRetryAttemptsForAnEvent;
  String lockId;
}
