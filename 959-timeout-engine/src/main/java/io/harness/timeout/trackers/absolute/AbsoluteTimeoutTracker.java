/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timeout.trackers.absolute;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerState;
import io.harness.timeout.contracts.Dimension;

import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@TypeAlias("absoluteTimeoutTracker")
public class AbsoluteTimeoutTracker implements TimeoutTracker {
  private long timeoutMillis;
  private long startTimeMillis;

  public AbsoluteTimeoutTracker(long timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
    this.startTimeMillis = System.currentTimeMillis();
  }

  @Override
  public Dimension getDimension() {
    return AbsoluteTimeoutTrackerFactory.DIMENSION;
  }

  @Override
  public Long getExpiryTime() {
    return startTimeMillis + timeoutMillis;
  }

  @Override
  public TimeoutTrackerState getState() {
    return System.currentTimeMillis() > startTimeMillis + timeoutMillis ? TimeoutTrackerState.EXPIRED
                                                                        : TimeoutTrackerState.TICKING;
  }
}
