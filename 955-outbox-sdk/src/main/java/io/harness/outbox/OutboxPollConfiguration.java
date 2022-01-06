/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
