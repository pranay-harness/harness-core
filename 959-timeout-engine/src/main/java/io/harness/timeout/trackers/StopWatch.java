/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timeout.trackers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(CDC)
public class StopWatch {
  private long startTimeMillis;
  private long elapsedMillis;
  @Getter private boolean ticking;

  public StopWatch(boolean ticking) {
    this.startTimeMillis = System.currentTimeMillis();
    this.elapsedMillis = 0;
    this.ticking = ticking;
  }

  public void pause() {
    if (!ticking) {
      return;
    }

    long now = System.currentTimeMillis();
    elapsedMillis += Math.max(0, now - startTimeMillis);
    ticking = false;
  }

  public void resume() {
    if (ticking) {
      return;
    }

    startTimeMillis = System.currentTimeMillis();
    ticking = true;
  }

  public long getElapsedMillis() {
    if (ticking) {
      return elapsedMillis + Math.max(0, System.currentTimeMillis() - startTimeMillis);
    } else {
      return elapsedMillis;
    }
  }
}
