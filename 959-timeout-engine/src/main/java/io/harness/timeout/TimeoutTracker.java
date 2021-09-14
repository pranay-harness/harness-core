/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.contracts.Dimension;

@OwnedBy(CDC)
public interface TimeoutTracker {
  Dimension getDimension();
  Long getExpiryTime();
  TimeoutTrackerState getState();

  /**
   * onEvent callback is invoked whenever an event occurs. Example: node execution status update.
   *
   * @param event the event that occurred
   * @return true if any field of the tracker changed and DB needs to be updated, false otherwise
   */
  default boolean onEvent(TimeoutEvent event) {
    // Ignore all events by default.
    return false;
  }
}
