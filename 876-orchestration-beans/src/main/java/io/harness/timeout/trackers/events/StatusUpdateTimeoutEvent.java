/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout.trackers.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.Status;
import io.harness.timeout.TimeoutEvent;

import lombok.AllArgsConstructor;
import lombok.Value;

@OwnedBy(CDC)
@Value
@AllArgsConstructor
public class StatusUpdateTimeoutEvent implements TimeoutEvent {
  public static final String EVENT_TYPE = "STATUS_UPDATE";

  Status status;

  @Override
  public String getType() {
    return EVENT_TYPE;
  }
}
