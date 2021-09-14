/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
