/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
